package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.CappedPeriodException;
import net.officefloor.hq.app.period.ClosedPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodCap;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;

/** Check every pasted line against the sheet first, as the claim form would for its person: the name must be on
 *  the sheet (compared as names are), the claim not dated (a blank date being the sheet's entry date, else its today) after the person's
 *  retirement date, its reference (if any) not already used on the sheet, nor due by (its date plus the terms it would get) inside a closed period. Any failure refuses the whole paste, naming the line, and nothing
 *  is written. Otherwise each line, in order, is added just as the claim form adds one (its CLAIM_ADDED line) — or, when
 *  the person has a claim of the line's date and note, that claim is updated: a DRAFT is edited to the line's amount
 *  (CLAIM_EDITED when it changes), a claim of any other status refuses the paste — and then opened by the user as the
 *  open button would open it (numbered in paste order, its CLAIM_OPENED line, then how the sheet's figures moved over
 *  the line). Any refusal the open button would give refuses the whole paste: nothing stored is kept and no audit line
 *  is written. */
public class PasteClaims {
    public void paste(@PathVariable("sheetId") String sheetId, @Val PasteInput input, PersonRepository people,
            SheetRepository sheets, SettingRepository settings, HolidayRepository holidays, PeriodRepository periods, ClaimRepository claims,
            ClaimLogRepository log, ClaimLineRepository lines, RateRepository rates, RoleRepository roles, Rollups rollups,
            Audit audit, PlatformTransactionManager transactions, Out<PasteResponse> result)
            throws InvalidClaimException {
        String sheet = sheetId.trim();
        periods.lockSheet(sheet);
        List<Person> owners = new ArrayList<>();
        for (PasteLine line : input.lines()) {
            Person person = people.findByName(sheet, line.name());
            if (person == null) {
                throw new InvalidClaimException("Line " + line.number() + ": " + line.name() + " is not on the sheet.");
            }
            if (line.claim().ref() != null && claims.refUsed(sheet, line.claim().ref(), 0)) {
                throw new InvalidClaimException("Line " + line.number() + ": " + RefuseUsedNewRef.message(line.claim().ref()));
            }
            var date = line.claim().date() != null ? line.claim().date() : sheets.newClaimDate(sheet);
            try {
                RetiredPersonException.refuse(person, date);
            } catch (RetiredPersonException e) {
                throw new InvalidClaimException("Line " + line.number() + ": " + e.getMessage());
            }
            Period closed = periods.closedOn(sheet, DueBy.of(person, date, settings, holidays));
            if (closed != null) {
                throw new InvalidClaimException("Line " + line.number() + ": " + new ClosedPeriodException(closed).getMessage());
            }
            owners.add(person);
        }
        // The writes run in a transaction of their own, so a refused line undoes what the lines before it stored.
        TransactionTemplate transaction = new TransactionTemplate(transactions);
        audit.hold();
        try {
            InvalidClaimException refused = transaction.execute(status -> {
                for (int i = 0; i < owners.size(); i++) {
                    PasteLine line = input.lines().get(i);
                    try {
                        paste(sheet, input.user(), line.claim(), owners.get(i), people, sheets, settings, holidays,
                                periods, claims, log, lines, rates, roles, rollups, audit);
                    } catch (InvalidClaimException | ClaimStatusNotAllowedException | ClosedPeriodException
                            | CappedPeriodException e) {
                        status.setRollbackOnly();
                        return new InvalidClaimException("Line " + line.number() + ": " + e.getMessage());
                    }
                }
                return null;
            });
            if (refused != null) {
                throw refused;
            }
            audit.release();
        } finally {
            audit.drop();
        }
        result.set(new PasteResponse(owners.size()));
    }

    /** Add one line's claim, or update the person's DRAFT matching it, and open it as the open button would. */
    private static void paste(String sheet, String user, ClaimInput line, Person person, PersonRepository people,
            SheetRepository sheets, SettingRepository settings, HolidayRepository holidays, PeriodRepository periods,
            ClaimRepository claims, ClaimLogRepository log, ClaimLineRepository lines, RateRepository rates,
            RoleRepository roles, Rollups rollups, Audit audit)
            throws InvalidClaimException, ClaimStatusNotAllowedException, ClosedPeriodException, CappedPeriodException {
        var date = line.date() != null ? line.date() : sheets.newClaimDate(sheet);
        String note = line.note() == null ? "" : line.note();
        List<Claim> matching = claims.listForPerson(sheet, person.id()).stream()
                .filter(c -> date.equals(c.date()) && note.equals(c.note() == null ? "" : c.note())).toList();
        Claim other = matching.stream().filter(c -> c.status() != ClaimStatus.DRAFT).findFirst().orElse(null);
        if (other != null) {
            throw new InvalidClaimException(person.name() + " already has a " + other.status()
                    + " claim dated " + date + " with this note; only a DRAFT can be updated by a paste.");
        }
        Rollup before = rollups.take(sheet);
        Claim claim;
        if (matching.isEmpty()) {
            claim = SaveNewClaim.insert(line, person, sheets, settings, holidays, periods, claims, log);
            AuditClaimAdded.record(line, person, claim, audit);
        } else {
            claim = matching.get(0);
            String holder = claims.lockedBy(claim);
            if (holder != null && !holder.equals(user)) {
                throw new ClaimStatusNotAllowedException("This claim is being edited by " + holder
                        + "; it cannot be edited until they save or cancel.");
            }
            if (claim.amount().compareTo(line.amount()) != 0) {
                var total = lines.total(claim);
                if (total != null && total.compareTo(line.amount()) != 0) {
                    throw new InvalidClaimException("This claim has lines; its amount is their total, "
                            + total.toPlainString() + ", and cannot be typed.");
                }
                Claim edited = new Claim(claim.id(), claim.sheetId(), claim.personId(), line.amount(), claim.date(),
                        claim.note(), claim.status(), claim.number(), claim.settledDue(), claim.openedBy(),
                        claim.stated(), claim.statedDue(), claim.corrects(), claim.dueBy(), claim.deferredUntil(),
                        claim.proofs(), claim.splitFrom(), claim.lateEntry(), claim.reminded(),
                        line.ref() != null ? line.ref() : claim.ref(), claim.tags(), claim.waived(), claim.photos());
                claims.update(edited, user);
                claim = claims.find(sheet, claim.id());
                ClaimResponse shown = ClaimResponse.from(claim);
                audit.record("CLAIM_EDITED sheet=" + sheet + " by=" + user + " person=" + person.name()
                        + " amount=" + shown.amount() + " date=" + shown.date()
                        + (shown.number() != null ? " number=" + shown.number() : ""));
            }
        }
        Period closed = periods.closedOn(sheet, claim.dueBy());
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
        Period capped = PeriodCap.reached(claim, periods, claims);
        if (capped != null) {
            throw new CappedPeriodException(capped);
        }
        ClaimStatusInput open = new ClaimStatusInput(user, "open", ClaimStatus.OPEN, null);
        ClaimResponse saved = ClaimStatusChange.save(open, claim, claims, people, rates, roles, settings, sheets, log);
        ClaimStatusChange.audit(open, claim, saved, people, null, audit);
        rollups.record(sheet, user, before);
    }
}
