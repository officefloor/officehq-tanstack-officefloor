package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.hq.app.voidreason.VoidReasonStore;
import net.officefloor.hq.app.watch.WatchStore;
import net.officefloor.plugin.variable.Val;
import net.officefloor.plugin.variable.Out;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;

/** Settles each selected claim, in list order: its people by their positions, a person's
 *  claims newest first. Each goes through the very steps of a single settle by the user (period,
 *  clerk limit, opened-by, edited-by and status checks, then store, log and audit), so a clerk's settle above
 *  the clerk limit sends it to PENDING, which counts as settled. A claim that is not on the sheet,
 *  or that a single settle would refuse (every claim on an archived sheet), is refused.
 *  All or nothing: when any is refused, none is settled, every one counts as refused and nothing is
 *  stored or audited. Otherwise the bulk settle takes the sheet's next batch, each settled claim records
 *  it, and after the claims' own records BATCH_SETTLED sheet=<id> by=<user> batch=<n> count=<claims>
 *  total=<the frozen figures of those SETTLED, added up> is written. */
public class SettleClaims {
    public void settle(@PathVariable("sheetId") String sheetId, @Val BulkSettleInput input, ClaimStore store,
            PersonStore people, PeriodStore periods, SettingStore settings, SheetStore sheets, RoleStore roles,
            RateStore rates, ClaimLogStore log, RollupService rollups, Audit audit, WatchStore watches,
            VoidReasonStore voidReasons, PlatformTransactionManager transactions,
            Out<BulkSettleResult> result) {
        String sheet = sheetId.trim();
        Map<Long, Integer> personOrder = new HashMap<>();
        // A person's claims newest first, as listed: a claim moved to them is listed as their newest.
        Map<Long, Integer> listed = new HashMap<>();
        for (Claim claim : store.list(sheet)) {
            listed.put(claim.getId(), listed.size());
        }
        for (Person person : people.list(sheet)) {
            personOrder.put(person.getId(), personOrder.size());
        }
        int refused = 0;
        List<Claim> selected = new ArrayList<>();
        for (Long id : input.claimIds()) {
            Claim claim = id == null ? null : store.find(sheet, id);
            if (claim == null) {
                refused++;
            } else {
                selected.add(claim);
            }
        }
        selected.sort(Comparator.<Claim>comparingInt(c -> personOrder.getOrDefault(c.getPersonId(), Integer.MAX_VALUE))
                .thenComparing(c -> listed.getOrDefault(c.getId(), Integer.MAX_VALUE)));
        StatusInput status = new StatusInput(ClaimStatus.SETTLED, input.user(), false, "");
        int all = input.claimIds().size();
        if (refused > 0) {
            result.set(new BulkSettleResult(0, all));
            return;
        }
        // All or nothing: the claims are settled in a nested transaction, their audit records held back,
        // and the first refusal rolls every one of them back and drops the records.
        TransactionTemplate nested = new TransactionTemplate(transactions);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        audit.hold();
        boolean done = false;
        try {
            done = Boolean.TRUE.equals(nested.execute(tx -> {
                long batch = store.nextBatch(sheet);
                BigDecimal total = BigDecimal.ZERO;
                for (Claim claim : selected) {
                    try {
                        sheets.refuseArchived(sheet);
                        new CheckClaimPeriod().check(claim, periods);
                        StatusMove[] move = new StatusMove[1];
                        new CheckClerkLimit().check(status, claim, roles, settings, people, rates, sheets, store, log,
                                m -> move[0] = m);
                        Rollup before = rollups.snapshot(sheet);
                        StatusChange[] change = new StatusChange[1];
                        new ApplyStatus().apply(status, move[0], claim, store, people, rates, settings, sheets, periods,
                                voidReasons, c -> change[0] = c);
                        new SaveClaim().save(status, claim, store, log);
                        store.markBatch(claim, batch);
                        new AuditClaimStatus().audit(status, move[0], claim, change[0], before, people, audit, rollups,
                                watches);
                        if (move[0].to() == ClaimStatus.SETTLED && claim.getSettledDue() != null) {
                            total = total.add(claim.getSettledDue());
                        }
                    } catch (InvalidSheetException e) {
                        tx.setRollbackOnly();
                        return false;
                    }
                }
                audit.record("BATCH_SETTLED sheet=" + sheet + " by=" + input.user() + " batch=" + batch + " count="
                        + selected.size() + " total=" + total.setScale(2, RoundingMode.HALF_UP).toPlainString());
                return true;
            }));
        } finally {
            if (done) {
                audit.release();
            } else {
                audit.discard();
            }
        }
        result.set(done ? new BulkSettleResult(selected.size(), 0) : new BulkSettleResult(0, all));
    }
}
