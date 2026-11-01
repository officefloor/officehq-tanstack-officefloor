package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRetiredException;
import net.officefloor.hq.app.person.Watches;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Snapshot;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.hq.app.voidreason.VoidReasons;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;

/** Each pasted line, in order, as the claim form and the open button would for its named person:
 *  a line whose date and note match a claim of that person as they stood before the paste (no VOID
 *  or DELETED one) updates the newest such DRAFT to its amount (CLAIM_EDITED when that changes),
 *  else is added as a new claim (CLAIM_ADDED); either is then opened (CLAIM_OPENED, numbered in paste
 *  order), then the figures it moved are audited. A line whose matches hold no DRAFT, or any
 *  refusal the add, edit or open would give, refuses the whole paste: its own transaction is
 *  rolled back and the held audit lines are never written. */
public class AddPastedClaims {
    public void add(@PathVariable("sheetId") String sheetId, @Val PastedClaims paste, People people, Sheets sheets,
            ClosedPeriods closedPeriods, Claims claims, Settings settings, Holidays holidays, ClaimLog log, Audit audit,
            ClaimNumbers numbers, Figures figures, Rollup rollup, Watches watches, VoidReasons voidReasons,
            ClaimLines claimLines, PlatformTransactionManager transactionManager, Out<PasteClaimsResult> result)
            throws PeriodClosedException, PersonRetiredException, InvalidSheetRequestException,
            ClaimStatusRefusedException, ClaimLockedException {
        BufferedAudit held = new BufferedAudit(settings);
        // In a transaction of its own, rolled back when any line is refused: nothing is kept.
        Exception refused = new TransactionTemplate(transactionManager).execute(status -> {
            try {
                paste(sheetId, paste, people, sheets, closedPeriods, claims, settings, holidays, log, held, numbers,
                        figures, rollup, watches, voidReasons, claimLines);
                return null;
            } catch (PeriodClosedException | PersonRetiredException | InvalidSheetRequestException
                    | ClaimStatusRefusedException | ClaimLockedException e) {
                status.setRollbackOnly();
                return e;
            }
        });
        if (refused instanceof PeriodClosedException e) throw e;
        if (refused instanceof PersonRetiredException e) throw e;
        if (refused instanceof InvalidSheetRequestException e) throw e;
        if (refused instanceof ClaimStatusRefusedException e) throw e;
        if (refused instanceof ClaimLockedException e) throw e;
        held.flush(audit);
        result.set(new PasteClaimsResult(paste.lines().size()));
    }

    private static void paste(String sheetId, PastedClaims paste, People people, Sheets sheets,
            ClosedPeriods closedPeriods, Claims claims, Settings settings, Holidays holidays, ClaimLog log,
            BufferedAudit held, ClaimNumbers numbers, Figures figures, Rollup rollup, Watches watches,
            VoidReasons voidReasons, ClaimLines claimLines)
            throws PeriodClosedException, PersonRetiredException, InvalidSheetRequestException,
            ClaimStatusRefusedException, ClaimLockedException {
        List<Claim> before = claims.onSheet(sheetId);
        ClaimStatusChange open = new ClaimStatusChange(ClaimAction.OPEN, paste.user());
        ApplyClaimStatus apply = new ApplyClaimStatus();
        RecordClaimAdded added = new RecordClaimAdded();
        RecordClaimStatusChanged opened = new RecordClaimStatusChanged();
        Set<Long> used = new HashSet<>();
        int n = 0;
        for (PastedLine line : paste.lines()) {
            n++;
            Person person = people.named(sheetId, line.name());
            if (person == null) throw new InvalidSheetRequestException("No one on the sheet is named " + line.name());
            ClaimValues values = line.values();
            LocalDate date = values.date() != null ? values.date() : settings.claimDate(sheetId);
            Claim draft = null;
            boolean matched = false;
            for (Claim was : before) { // newest first
                if (was.getPersonId() != person.getId() || !was.getDate().equals(date)
                        || !note(was.getNote()).equals(note(values.note()))
                        || was.getStatus() == ClaimStatus.VOID || was.getStatus() == ClaimStatus.DELETED) continue;
                matched = true;
                if (was.getStatus() == ClaimStatus.DRAFT && !used.contains(was.getId())) {
                    draft = was;
                    break;
                }
            }
            if (matched && draft == null) {
                throw new InvalidSheetRequestException("Line " + n + ": " + line.name()
                        + " has a claim on " + date + " with that note, but none still a DRAFT");
            }
            Snapshot snapshot = rollup.snapshot(sheetId);
            Claim claim;
            if (draft != null) {
                used.add(draft.getId());
                claim = claims.find(sheetId, draft.getId());
                ApplyClaimLock.refuseLocked(claim, paste.user());
                if (values.amount().compareTo(claim.getAmount()) != 0) {
                    if (claimLines.any(claim)) {
                        throw new InvalidSheetRequestException("Line " + n + ": a claim with lines takes its amount from them: "
                                + claim.getAmount().toPlainString());
                    }
                    closedPeriods.refuse(sheetId, claim.getDueBy());
                    claim.setAmount(values.amount());
                    claim.setEditedBy(paste.user());
                    claims.save(claim);
                    held.record("CLAIM_EDITED sheet=" + sheetId + " by=" + paste.user()
                            + " person=" + person.getName() + " amount=" + claim.getAmount().toPlainString()
                            + " date=" + claim.getDate() + (claim.getNumber() != null ? " number=" + claim.getNumber() : ""));
                }
            } else {
                claim = BuildClaim.built(values, person, sheets, closedPeriods, claims, settings, holidays);
                claims.save(claim);
                added.record(values, person, claim, log, held);
            }
            apply.apply(open, claim, numbers, people, figures, closedPeriods, settings, log, sheets, voidReasons);
            claims.save(claim);
            opened.record(open, claim, people, log, watches, held);
            rollup.record(sheetId, snapshot, paste.user(), held);
        }
    }

    private static String note(String note) {
        return Objects.requireNonNullElse(note, "").trim();
    }
}
