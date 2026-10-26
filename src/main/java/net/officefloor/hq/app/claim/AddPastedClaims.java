package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.WatchRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupSnapshot;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Takes each pasted line, in order, for the person of that name (case and surrounding spaces ignored), and opens its
 * claim as the open button would. A line whose date (blank: the sheet's today) and note match one of the person's
 * claims as they stood before the paste updates that claim instead of adding one: a DRAFT takes the line's amount
 * (CLAIM_EDITED when it changes) and is opened; a match in any other status refuses the paste. Two lines of one paste
 * never match each other. Every line is first checked as the claim form checks a new claim (person on the sheet,
 * correction, reference, closed period, retirement) or an edit; then each claim is added (CLAIM_ADDED) or edited,
 * opened (CLAIM_OPENED, numbered in paste order) and its rollup lines written. Any refusal — a check or an open the
 * open button would refuse — refuses them all: nothing is written to the store or the audit file.
 */
public class AddPastedClaims {

    public void add(@Val SheetResponse sheet, @Val PasteLines pasted, @Val ChangedBy by, PersonRepository people,
            ClaimRepository claims, SheetRepository sheets, PeriodRepository periods, SettingRepository settings,
            HolidayRepository holidays, RateRepository rates, RoleRepository roles, ClaimLogRepository logs,
            ClaimLockRepository locks, ClaimLineRepository claimLines, WatchRepository watches,
            VoidReasonRepository voidReasons, Rollup rollup, Audit audit, ClaimLog log, Out<PasteResponse> response)
            throws BadRequestException {
        LocalDate today = settings.entryDate(sheet.id(), sheets.today(sheet.id()));
        List<Person> persons = new ArrayList<>();
        List<Claim> matched = new ArrayList<>();
        Set<Long> taken = new HashSet<>();
        int n = 0;
        for (PasteLine line : pasted.lines()) {
            n++;
            Person person = people.findByName(sheet.id(), line.name());
            if (person == null) {
                throw new BadRequestException("Line " + n + ": " + line.name() + " is not on this sheet");
            }
            ClaimInput input = line.input();
            LocalDate date = input.date() != null ? input.date() : today;
            Claim match = null;
            try {
                for (Claim claim : claims.listOf(sheet.id(), person.getId())) {
                    if (date.equals(claim.getDate()) && noteOf(input.note()).equals(noteOf(claim.getNote()))) {
                        if (claim.getStatus() != ClaimStatus.DRAFT) {
                            throw new BadRequestException("it matches a " + claim.getStatus() + " claim");
                        }
                        if (match == null) {
                            match = claim;
                        }
                    }
                }
                if (match == null) {
                    RefuseBadCorrection.refuse(input, sheet.id(), person.getId(), claims);
                    RefuseUsedRef.refuse(sheet.id(), input.ref(), -1, claims);
                    RefuseClosedDate.refuse(sheet.id(), BuildClaim.dueBy(person, date, settings, holidays), periods);
                    RefuseRetiredDate.refuse(person, date);
                } else {
                    if (!taken.add(match.getId())) {
                        throw new BadRequestException("it matches the same claim as an earlier line");
                    }
                    String holder = locks.holder(match.getId());
                    if (holder != null && !holder.equals(by.name())) {
                        throw new BadRequestException(ApplyClaimLock.locked(holder));
                    }
                    RefuseUsedRef.refuse(sheet.id(), input.ref(), match.getId(), claims);
                    new RefuseClosedClaim().check(match, periods);
                    new RefuseLinedAmount().check(match, input, claimLines);
                }
            } catch (BadRequestException e) {
                throw new BadRequestException("Line " + n + ": " + e.getMessage());
            }
            persons.add(person);
            matched.add(match);
        }

        ApplyClaimStatus apply = new ApplyClaimStatus();
        RecordClaimStatus record = new RecordClaimStatus();
        String[] refused = new String[1];
        boolean kept = false;
        audit.hold();
        try {
            kept = claims.keepOnly(() -> {
                for (int i = 0; i < persons.size(); i++) {
                    Person person = persons.get(i);
                    ClaimInput input = pasted.lines().get(i).input();
                    RollupSnapshot before = rollup.snapshot(sheet.id());
                    Claim claim = matched.get(i);
                    if (claim == null) {
                        LocalDate date = input.date() != null ? input.date() : today;
                        claim = claims.insert(sheet.id(), person.getId(), input.amount(), date, input.note(),
                                input.corrects(), input.ref(), input.tags(), BuildClaim.terms(person, date, settings));
                        if (periods.softClosedOn(sheet.id(), claim.getDueBy())) {
                            claims.markLate(claim);
                        }
                        audit.record(ClaimAudit.added(claim, by, person.getName()));
                        if (claim.isLateEntry()) {
                            audit.record(ClaimAudit.line("LATE_ENTRY", claim, by, person.getName()));
                        }
                        log.record(claim, by);
                    } else if (claim.getAmount().compareTo(input.amount()) != 0
                            || (input.ref() != null && !Objects.equals(input.ref(), claim.getRef()))) {
                        boolean amountChanged = claim.getAmount().compareTo(input.amount()) != 0;
                        claim.setAmount(input.amount());
                        if (input.ref() != null) {
                            claim.setRef(input.ref());
                        }
                        claim.setEditedBy(by.name());
                        claims.update(claim);
                        if (amountChanged) {
                            audit.record(ClaimAudit.line("CLAIM_EDITED", claim, by, person.getName()));
                        }
                    }
                    try {
                        ClaimNumber[] number = new ClaimNumber[1];
                        Out<ClaimNumber> audited = value -> number[0] = value;
                        apply.apply(ClaimStatus.OPEN, claim, claims, people, rates, roles, settings, sheets, periods, logs,
                                by, ClaimReview.NONE, VoidChoice.NONE, voidReasons, audited);
                        record.record(claim, number[0], by, ClaimReview.NONE, people, watches, logs, audit, log);
                    } catch (BadRequestException e) {
                        // One refusal refuses them all: nothing is added, edited or opened.
                        refused[0] = "Line " + (i + 1) + ": " + e.getMessage();
                        return false;
                    }
                    rollup.record(sheet.id(), before, by);
                }
                return true;
            });
        } finally {
            audit.release(kept);
        }
        if (!kept) {
            throw new BadRequestException(refused[0] != null ? refused[0] : "The paste was refused");
        }
        response.set(new PasteResponse(persons.size()));
    }

    private static String noteOf(String note) {
        return note == null ? "" : note.trim();
    }
}
