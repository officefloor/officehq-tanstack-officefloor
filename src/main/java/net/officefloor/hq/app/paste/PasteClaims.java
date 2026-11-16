package net.officefloor.hq.app.paste;

import java.math.BigDecimal;
import net.officefloor.hq.app.pool.PoolRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimLineRepository;
import net.officefloor.hq.app.claim.ClaimLockedException;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.ClaimStatusException;
import net.officefloor.hq.app.claim.Corrections;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.OpenChecks;
import net.officefloor.hq.app.claim.ValidClaim;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Each pasted line, in order, puts an OPEN claim on the person it names (compared as names are).
 *  A line whose date and note match claims of that person as they stood before the paste (void and
 *  deleted ones never match) updates the newest DRAFT of them instead: edited to the line's amount
 *  (CLAIM_EDITED when it changes) and opened; a line whose matches include no DRAFT is refused. Any
 *  other line is first checked as {@link net.officefloor.hq.app.claim.AddClaim} checks a claim (its
 *  person on the sheet, not retired before its date, its due-by outside a closed period, its reference
 *  used by no claim on the sheet nor an earlier line), then added (CLAIM_ADDED) and opened. Each claim is
 *  opened as {@link net.officefloor.hq.app.claim.OpenClaim} opens one — numbered in paste order, CLAIM_OPENED then the rollup lines it
 *  moved — and any refusal it would give refuses the whole paste: the transaction is rolled back and the
 *  audit lines, kept until every line went through, are never written. A paste that would take a person
 *  past the sheet's maximum of open claims is refused before anything is written. */
public class PasteClaims {
    private record Pending(Person person, ValidClaim claim, LocalDate date, LocalDate dueBy, String corrects,
            Claim match) {
    }

    public void paste(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val PasteLines lines,
            PersonRepository people, SheetRepository sheets, ClaimRepository claims,
            PeriodRepository periods, SettingRepository settings, PoolRepository pools, ClaimLogRepository log, Audit audit,
            HolidayRepository holidays, RoleRepository roles, RateRepository rates, ClaimLineRepository claimLines,
            Out<PasteResult> result, PlatformTransactionManager transactions)
            throws PeriodClosedException, InvalidClaimException, ClaimStatusException, ClaimLockedException {
        // Run in a transaction of its own: a line refused after earlier lines were written undoes them all.
        TransactionStatus tx = transactions.getTransaction(new DefaultTransactionDefinition());
        try {
            pasteAll(sheetId, by, lines, people, sheets, claims, periods, settings, pools, log, audit, holidays, roles, rates,
                    claimLines, result);
        } catch (Throwable e) {
            transactions.rollback(tx);
            throw e;
        }
        transactions.commit(tx);
    }

    private void pasteAll(String sheetId,
            String by, PasteLines lines,
            PersonRepository people, SheetRepository sheets, ClaimRepository claims,
            PeriodRepository periods, SettingRepository settings, PoolRepository pools, ClaimLogRepository log, Audit audit,
            HolidayRepository holidays, RoleRepository roles, RateRepository rates, ClaimLineRepository claimLines,
            Out<PasteResult> result)
            throws PeriodClosedException, InvalidClaimException, ClaimStatusException, ClaimLockedException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        if (!Permissions.allows(id, user, Permissions.OPEN, roles, settings)) {
            throw new ClaimStatusException("Your role may not open a claim");
        }
        List<Pending> pending = new ArrayList<>();
        Set<String> refs = new HashSet<>();
        Integer maxOpen = settings.maxOpen(id);
        Map<Long, Long> opening = new HashMap<>();
        for (PasteLine line : lines.lines()) {
            Person person = people.findByName(id, line.name());
            if (person == null) {
                throw new InvalidClaimException("Not on the sheet: " + line.name());
            }
            // The maximum of open claims is checked before anything is written: each line opens one more.
            if (maxOpen != null) {
                long held = opening.computeIfAbsent(person.getId(), (p) -> claims.listOf(id, p).stream()
                        .filter((c) -> ClaimStatus.isOpen(c.getStatus())).count());
                if (held >= maxOpen) {
                    throw new ClaimStatusException("The person already has " + maxOpen + " open claims");
                }
                opening.put(person.getId(), held + 1);
            }
            ValidClaim request = line.claim();
            LocalDate date = request.date() != null ? request.date() : settings.entryDate(id);
            Claim match = match(claims.listOf(id, person.getId()), date, request.note());
            if (match != null) {
                pending.add(new Pending(person, request, date, null, null, match));
                continue;
            }
            if (person.retiredBefore(date)) {
                throw new InvalidClaimException("The person is retired from " + person.getRetiredFrom());
            }
            // The due-by is fixed now, by the terms in force on the sheet's today,
            // counted in business days past the weekends and the sheet's holidays as they are now.
            LocalDate dueBy = holidays.dueBy(id, date, settings.termsToday(id, person));
            periods.requireOpen(id, dueBy.toString());
            String corrects = Corrections.check(request, id, person.getId(), claims);
            // A reference is used once on the sheet: also once within the paste.
            claims.requireRefFree(id, request.ref(), 0);
            if (request.ref() != null && !refs.add(request.ref().toLowerCase(Locale.ROOT))) {
                throw new InvalidClaimException("The reference is repeated in the paste: " + request.ref());
            }
            pending.add(new Pending(person, request, date, dueBy, corrects, null));
        }

        List<String> records = new ArrayList<>();
        Rollup before = Rollup.of(id, people, claims, rates, sheets, settings, pools);
        for (Pending p : pending) {
            String head = " sheet=" + id + " by=" + user + " person=" + p.person().getName();
            Claim claim;
            if (p.match() != null) {
                claim = claims.find(id, p.match().getId());
                // An earlier line of the paste may have opened it already.
                if (!ClaimStatus.DRAFT.name().equals(claim.getStatus())) {
                    throw new InvalidClaimException("No DRAFT claim of " + p.person().getName() + " matches the line");
                }
                if (claim.getLockedBy() != null && !claim.getLockedBy().equals(user)) {
                    throw new ClaimLockedException("The claim is being edited by " + claim.getLockedBy());
                }
                BigDecimal amount = p.claim().amount();
                if (new BigDecimal(claim.getAmount()).compareTo(amount) != 0) {
                    BigDecimal lined = claimLines.total(claim);
                    if (lined != null && lined.compareTo(amount) != 0) {
                        throw new InvalidClaimException("A claim with lines has their total as its amount: "
                                + lined.toPlainString());
                    }
                    periods.requireOpen(id, claim.getDueBy());
                    claim.setAmount(amount.toPlainString());
                    claim.setEditedBy(user);
                    claims.update(claim);
                    records.add("CLAIM_EDITED" + head + " amount=" + claim.getAmount() + " date=" + claim.getDate()
                            + claim.getAuditNumber());
                }
            } else {
                claim = claims.add(id, p.person().getId(), p.claim().amount(), p.date(),
                        p.claim().note(), p.corrects(), p.dueBy(), user, p.claim().ref());
                if (periods.softClosedOn(id, claim.getDueBy())) claims.markLateEntry(claim);
                log.record(claim, user);
                records.add("CLAIM_ADDED" + head + " amount=" + claim.getAmount() + " date=" + claim.getDate()
                        + claim.getAuditRef());
                if (claim.isLateEntry()) {
                    records.add("LATE_ENTRY" + head + " amount=" + claim.getAmount() + " date=" + claim.getDate());
                }
            }

            // Opened as the open button opens it.
            OpenChecks.requireFlagSteward(claim, user, people, roles);
            periods.requireOpen(id, claim.getDueBy());
            ClaimStatus opened = OpenChecks.opensTo(claim, people, rates, settings, sheets);
            OpenChecks.requireNote(claim, people, rates, settings, sheets);
            OpenChecks.requireUnderCap(claim, claims, periods);
            OpenChecks.requireUnderMaxOpen(claim, claims, settings);
            claim.setStatus(opened.name());
            claim.setOpenedBy(user);
            claims.update(claim);
            log.record(claim, user);
            records.add(opened.event() + head + " amount=" + claim.getAmount() + " date=" + claim.getDate()
                    + claim.getAuditNumber());
            Rollup after = Rollup.of(id, people, claims, rates, sheets, settings, pools);
            records.addAll(after.changesSince(before, id, user));
            before = after;
        }
        // Every line went through: only now is anything recorded.
        for (String record : records) {
            audit.record(record);
        }
        result.set(new PasteResult(pending.size()));
    }

    /** The newest DRAFT of the person's claims (newest first) on the date with the note (blank: none);
     *  null when none of them matches; refused when they match only claims that are not DRAFT. */
    private static Claim match(List<Claim> theirs, LocalDate date, String note) throws InvalidClaimException {
        String wanted = note == null ? "" : note.trim();
        boolean matched = false;
        for (Claim c : theirs) {
            if (ClaimStatus.VOID.name().equals(c.getStatus()) || ClaimStatus.DELETED.name().equals(c.getStatus())) {
                continue;
            }
            String has = c.getNote() == null ? "" : c.getNote().trim();
            if (!date.toString().equals(c.getDate()) || !wanted.equals(has)) continue;
            if (ClaimStatus.DRAFT.name().equals(c.getStatus())) return c;
            matched = true;
        }
        if (matched) {
            throw new InvalidClaimException("The line matches a claim that is no longer a DRAFT");
        }
        return null;
    }
}
