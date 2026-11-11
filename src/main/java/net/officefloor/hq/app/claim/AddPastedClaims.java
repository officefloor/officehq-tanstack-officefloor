package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.CheckClaimPeriod;
import net.officefloor.hq.app.period.ClosedPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodCaps;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.CheckClaimRetired;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.hq.app.paste.PasteLine;
import net.officefloor.hq.app.paste.PasteLines;
import net.officefloor.hq.app.paste.PasteResponse;

/**
 * Adds and opens a claim per pasted line, in order, for the person on the sheet with that name (matched
 * as names are compared). A line naming the date and note of one of its person's claims as they stood
 * before the paste (not VOID nor DELETED, nor taken by an earlier line) updates that claim instead: the
 * newest such DRAFT is edited to the line's amount and opened; a line whose matches hold no DRAFT is
 * refused. Every line is checked first, as the claim form checks a claim (its person on the sheet, its
 * reference unused on the sheet and in the paste, its due-by outside a closed period and not after its
 * person's retirement) and as the editor checks an edit (not locked by another user, its lines' total), and its person under the sheet's maximum of open claims,
 * then each is opened as the open button opens it (the user's role allowed to open, a note when its due
 * needs one, its period's cap). One refusal refuses the whole paste: it is a single transaction, and
 * its audit lines are only written once every line has gone through. Each line writes CLAIM_ADDED (or
 * CLAIM_EDITED when its amount changed), then CLAIM_OPENED, then the figures it moved.
 */
public class AddPastedClaims {
    public void add(@Val Sheet sheet, @Val PasteLines paste, PersonRepository people, ClaimRepository claims,
            PeriodRepository periods, RateRepository rates, SettingsRepository settings, ClaimLogRepository log,
            ClaimLineRepository claimLines, HolidayRepository holidays, RoleRepository roles, SheetRepository sheets,
            HttpServletRequest request, Audit audit, Out<PasteResponse> result)
            throws InvalidClaimException, InvalidClaimStatusException, ClosedPeriodException,
            RetiredPersonException, ClaimLockedException {
        Settings terms = settings.find(sheet.id());
        String user = Audit.user(request);
        String role = roles.roleOf(sheet.id(), user);
        if (!terms.allows("open", role)) throw new InvalidClaimStatusException("A " + role + " may not open a claim");

        // Each line resolved against the claims as they stood before the paste.
        List<Person> persons = new ArrayList<>();
        List<Claim> built = new ArrayList<>();
        List<Boolean> existing = new ArrayList<>();
        List<Boolean> edited = new ArrayList<>();
        Map<Long, List<Claim>> before = new HashMap<>();
        Set<Long> taken = new HashSet<>();
        Set<String> refs = new HashSet<>();
        // Each person's OPEN or PENDING claims, counting the lines before that open one of theirs.
        Map<Long, Long> held = new HashMap<>();
        int number = 0;
        for (PasteLine line : paste.lines()) {
            number++;
            Person person = people.findByName(sheet.id(), line.name());
            if (person == null) throw new InvalidClaimException("No one named " + line.name() + " is on the sheet");
            if (person.flagRefuses(role, sheet.today())) {
                throw new InvalidClaimStatusException("Only a steward may open a claim of " + person.getName() + ": they are flagged");
            }
            LocalDate date = line.claim().date() == null ? terms.entryDay(sheet.today()) : line.claim().date();
            String note = line.claim().note() == null ? "" : line.claim().note();
            List<Claim> theirs = before.computeIfAbsent(person.getId(),
                    id -> claims.findByPerson(sheet.id(), id));
            boolean matched = false;
            Claim draft = null;
            for (Claim c : theirs) {
                if (c.getStatus() == ClaimStatus.VOID || c.getStatus() == ClaimStatus.DELETED) continue;
                if (taken.contains(c.getId())) continue;
                if (!date.equals(c.getDate())) continue;
                if (!note.equals(c.getNote() == null ? "" : c.getNote())) continue;
                matched = true;
                if (c.getStatus() == ClaimStatus.DRAFT) {
                    draft = c; // newest first
                    break;
                }
            }
            if (matched && draft == null) {
                throw new InvalidClaimException("Line " + number + ": " + line.name() + "'s claim on " + date
                        + " is no longer a draft");
            }
            Claim claim;
            if (draft != null) {
                claim = draft;
                taken.add(claim.getId());
                if (claims.lockedByOther(claim, user, sheet.today())) {
                    throw new ClaimLockedException(String.valueOf(claim.getId()));
                }
                new CheckClaimPeriod().check(sheet, claim, periods);
                BigDecimal total = claimLines.total(claim);
                if (total != null && total.compareTo(line.claim().amount()) != 0) {
                    throw new InvalidClaimException("A claim with lines takes their total as its amount");
                }
                boolean changed = claim.getAmount().compareTo(line.claim().amount()) != 0;
                if (changed) {
                    claim.setAmount(line.claim().amount());
                    claim.setEditedBy(user);
                }
                existing.add(true);
                edited.add(changed);
            } else {
                claim = new Claim(null, sheet.id(), person.getId(), line.claim().amount(), date, note,
                        ClaimStatus.DRAFT, null, null, null);
                claim.setRef(line.claim().ref());
                if (claim.getRef() != null && !refs.add(claim.getRef().toUpperCase(Locale.ROOT))) {
                    throw new InvalidClaimException("The reference " + claim.getRef() + " is pasted more than once");
                }
                new CheckClaimRef().check(claim, claims);
                claim.fixDueBy(terms.termsFor(person, sheet.today()), holidays.dates(sheet.id()));
                new CheckClaimPeriod().check(sheet, claim, periods);
                claim.setLateEntry(periods.softClosedOn(sheet.id(), claim.getDueBy()) != null);
                new CheckClaimRetired().check(sheet, claim, people);
                existing.add(false);
                edited.add(false);
            }
            // A note it needs to open is checked before anything is written.
            if (OpenClaim.needsNote(sheet, claim, people, rates, settings)) {
                throw new InvalidClaimStatusException(NoteRequired.MESSAGE);
            }
            if (terms.maxOpen() != null) {
                long now = held.computeIfAbsent(person.getId(), id -> MaxOpen.held(sheet, id, claims));
                if (now >= terms.maxOpen()) throw MaxOpen.refused(terms);
                if (OpenClaim.opensTo(sheet, claim, people, rates, settings) == ClaimStatus.OPEN) held.put(person.getId(), now + 1);
            }
            persons.add(person);
            built.add(claim);
        }

        // Then each, in paste order, is saved and opened as the open button opens it.
        List<String> lines = new ArrayList<>();
        Rollup figures = rollup(sheet, sheets, people, claims, rates, settings);
        for (int i = 0; i < built.size(); i++) {
            Claim claim = built.get(i);
            Person person = persons.get(i);
            if (!existing.get(i)) {
                claims.save(claim);
                log.record(claim, user, sheet.today(), LogClaimStatus.due(sheet, claim, people, rates, settings));
                lines.add(ClaimAudit.line("CLAIM_ADDED", sheet, user, person, claim)
                        + (claim.getRef() == null ? "" : " ref=" + claim.getRef())
                        + (claim.getTags() == null ? "" : " tags=" + claim.getTags()));
                if (claim.isLateEntry()) lines.add(ClaimAudit.line("LATE_ENTRY", sheet, user, person, claim));
            } else if (edited.get(i)) {
                claims.save(claim);
                lines.add(ClaimAudit.line("CLAIM_EDITED", sheet, user, person, claim));
            }
            Period full = PeriodCaps.full(sheet, claim, periods, claims);
            if (full != null) {
                throw new InvalidClaimStatusException("Period " + full.name() + " allows at most " + full.cap()
                        + " open claims per person");
            }
            claim.moveTo(OpenClaim.opensTo(sheet, claim, people, rates, settings));
            claim.setOpenedBy(user);
            if (!claims.markOpened(claim)) throw new InvalidClaimStatusException(ClaimStatus.DRAFT, ClaimStatus.OPEN);
            claims.save(claim);
            log.record(claim, user, sheet.today(), LogClaimStatus.due(sheet, claim, people, rates, settings));
            lines.add(ClaimAudit.line(claim.getStatus().auditEvent(), sheet, user, person, claim));
            Rollup after = rollup(sheet, sheets, people, claims, rates, settings);
            lines.addAll(after.changesSince(figures, sheet.id(), user));
            figures = after;
        }
        lines.forEach(audit::record);
        result.set(new PasteResponse(built.size()));
    }

    private static Rollup rollup(Sheet sheet, SheetRepository sheets, PersonRepository people, ClaimRepository claims,
            RateRepository rates, SettingsRepository settings) {
        return Rollup.of(sheet.id(), sheets.today(sheet.id()), people, claims, rates, settings.find(sheet.id()));
    }
}
