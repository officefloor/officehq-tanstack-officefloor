package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.Watches;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Takes the pasted claims in order, each arriving OPEN. Every line is checked against the sheet first
 * (the person is on the sheet, by name or alias, matched as names are compared; the date is not inside a
 * closed period nor after the person's retirement; a person flagged on the sheet's today only by a steward). A line whose person, date and note match one of the
 * person's claims as they stood before the paste (not VOID nor DELETED) updates the newest such DRAFT to
 * the line's amount (CLAIM_EDITED when it changes) instead of adding one; matches with no DRAFT among
 * them refuse the paste. Any other line is added as a DRAFT (CLAIM_ADDED). Each is then opened by the
 * user exactly as the open button would; a refusal there refuses the whole paste, which writes nothing.
 */
public class SavePasteClaims {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidPasteClaims request, People people, Claims claims,
            Sheets sheets, Periods periods, Audit audit, Rollup rollup, Settings settings, Rates rates,
            ClaimNumbering numbering, ClaimLog log, Watches watches, net.officefloor.hq.app.holiday.Holidays holidays,
            ObjectResponse<PasteClaimsResponse> response) throws InvalidClaimException {
        LocalDate today = settings.claimDate(sheetId);
        List<Claim> existing = claims.list(sheetId);
        List<Person> persons = new ArrayList<>();
        List<LocalDate> dates = new ArrayList<>();
        List<Claim> matched = new ArrayList<>();
        boolean steward = Settings.STEWARD.equals(settings.role(sheetId, request.user()));
        int number = 0;
        for (ValidPasteClaims.Line line : request.lines()) {
            number++;
            Person person = people.findByName(sheetId, line.name());
            if (person == null) {
                throw new InvalidClaimException("Claim " + number + ": no one on the sheet is named " + line.name());
            }
            if (!steward && person.flaggedOn(sheets.today(sheetId))) {
                throw new InvalidClaimException("Claim " + number + ": only a steward can open a claim of a flagged person");
            }
            LocalDate date = line.claim().date() != null ? line.claim().date() : today;
            LocalDate dueBy = holidays.dueBy(sheetId, date, settings.termsFor(sheetId, person.termsOn(date), person.gradeOn(date), date));
            if (periods.isClosed(sheetId, dueBy)) {
                throw new InvalidClaimException("Claim " + number + ": no claim can be added inside a closed period");
            }
            if (!person.allowsClaimOn(date)) {
                throw new InvalidClaimException("Claim " + number + ": no claim can be dated after the person's retirement date");
            }
            Claim match = match(existing, person, date, line.claim().note(), number);
            if (match == null) {
                try {
                    ValidateClaim.checkRef(line.claim().ref(), sheetId, null, claims);
                } catch (InvalidClaimException e) {
                    throw new InvalidClaimException("Claim " + number + ": " + e.getMessage());
                }
            }
            persons.add(person);
            dates.add(date);
            matched.add(match);
        }
        ValidClaimStatus open = new ValidClaimStatus(ClaimStatus.OPEN, request.user(), ValidClaimStatus.Decision.NONE, null);
        for (int i = 0; i < request.lines().size(); i++) {
            ValidClaim valid = request.lines().get(i).claim();
            Person person = persons.get(i);
            LocalDate date = dates.get(i);
            RollupFigures before = rollup.figures(sheetId);
            Claim claim = matched.get(i);
            if (claim == null) {
                claim = claims.add(sheetId, person.getId(), valid.amount(), date, valid.note(), null,
                        holidays.dueBy(sheetId, date, settings.termsFor(sheetId, person.termsOn(date), person.gradeOn(date), date)), valid.ref());
                log.record(claim, person, request.user());
                audit.recordOnCommit(ClaimAudit.added(request.user(), person, claim));
                LateEntry.mark(claim, person, request.user(), claims, periods, audit);
            } else {
                claim = claims.find(sheetId, claim.getId());
                if (claim.getStatus() != ClaimStatus.DRAFT) {
                    throw new InvalidClaimException("Claim " + (i + 1) + ": the claim it matches is no longer a DRAFT");
                }
                if (claim.getAmount().compareTo(valid.amount()) != 0) {
                    String lockedBy = claims.lockedBy(claim);
                    if (lockedBy != null && !lockedBy.equals(request.user())) {
                        throw new InvalidClaimException("Claim " + (i + 1) + ": the claim is being edited by " + lockedBy);
                    }
                    List<ClaimLine> lines = claims.lines(claim);
                    if (!lines.isEmpty() && valid.amount().compareTo(ClaimLine.sum(lines)) != 0) {
                        throw new InvalidClaimException("Claim " + (i + 1) + ": a claim with lines takes its amount from its lines");
                    }
                    claim.setAmount(valid.amount());
                    claim.setEditedBy(request.user());
                    claims.save(claim);
                    audit.recordOnCommit(ClaimAudit.line("CLAIM_EDITED", request.user(), person, claim, claim.getNumber()));
                }
            }
            ClaimNumber opened;
            try {
                ApplyClaimStatus.refuseOverCap(claim, periods, claims.list(sheetId));
                opened = ApplyClaimStatus.change(open, claim, sheets, people, rates, periods, settings, numbering, log);
            } catch (ClaimStatusRefusedException e) {
                throw new InvalidClaimException("Claim " + (i + 1) + ": " + e.getMessage());
            }
            claims.save(claim);
            AuditClaimStatus.record(open, claim, opened, people, audit, before, rollup, log, watches);
        }
        response.send(new PasteClaimsResponse(request.lines().size()));
    }

    /** The newest DRAFT among the person's claims (not VOID nor DELETED) of that date and note; null when none
     *  match; refused when some match but none is a DRAFT. */
    private static Claim match(List<Claim> existing, Person person, LocalDate date, String note, int number)
            throws InvalidClaimException {
        String wanted = note == null ? "" : note.trim();
        boolean any = false;
        Claim draft = null;
        for (Claim c : existing) {
            if (c.getPersonId() != person.getId() || c.getStatus() == ClaimStatus.VOID || c.getStatus() == ClaimStatus.DELETED
                    || !date.equals(c.getDate()) || !wanted.equals(c.getNote() == null ? "" : c.getNote().trim())) {
                continue;
            }
            any = true;
            if (c.getStatus() == ClaimStatus.DRAFT && (draft == null || c.getId() > draft.getId())) {
                draft = c;
            }
        }
        if (any && draft == null) {
            throw new InvalidClaimException("Claim " + number + ": the claims it matches include no DRAFT to update");
        }
        return draft;
    }
}
