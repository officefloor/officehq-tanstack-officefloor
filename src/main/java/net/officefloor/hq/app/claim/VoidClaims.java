package net.officefloor.hq.app.claim;

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
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;

/** Voids each selected claim, in list order: its people by their positions, a person's claims newest
 *  first. Each goes through the very steps of a single void by the user with the reason (viewer and
 *  archive, period, permission and status checks, the void reason while the sheet has any, then store,
 *  log and audit: CLAIM_VOIDED, then how it moved the sheet's figures). A claim that is not on the sheet,
 *  or that a single void would refuse, is refused: it is voided in a nested transaction with its audit
 *  records held back, so a refusal rolls it back and writes nothing. */
public class VoidClaims {
    public void voidClaims(@PathVariable("sheetId") String sheetId, @Val BulkVoidInput input, ClaimStore store,
            PersonStore people, PeriodStore periods, SettingStore settings, SheetStore sheets, RoleStore roles,
            RateStore rates, ClaimLogStore log, RollupService rollups, Audit audit, WatchStore watches,
            VoidReasonStore voidReasons, PlatformTransactionManager transactions,
            Out<BulkVoidResult> result) {
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
        StatusInput status = new StatusInput(ClaimStatus.VOID, input.user(), false, input.reason());
        TransactionTemplate nested = new TransactionTemplate(transactions);
        nested.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        int voided = 0;
        for (Claim claim : selected) {
            audit.hold();
            boolean done = false;
            try {
                done = Boolean.TRUE.equals(nested.execute(tx -> {
                    try {
                        roles.refuseChange(sheet, input.user());
                        new CheckClaimPeriod().check(claim, periods);
                        StatusMove[] move = new StatusMove[1];
                        new CheckClerkLimit().check(status, claim, roles, settings, people, rates, sheets, store, log,
                                m -> move[0] = m);
                        Rollup before = rollups.snapshot(sheet);
                        StatusChange[] change = new StatusChange[1];
                        new ApplyStatus().apply(status, move[0], claim, store, people, rates, settings, sheets, periods,
                                voidReasons, c -> change[0] = c);
                        new SaveClaim().save(status, claim, store, log);
                        new AuditClaimStatus().audit(status, move[0], claim, change[0], before, people, audit, rollups,
                                watches);
                        return true;
                    } catch (InvalidSheetException e) {
                        tx.setRollbackOnly();
                        return false;
                    }
                }));
            } finally {
                if (done) {
                    audit.release();
                } else {
                    audit.discard();
                }
            }
            if (done) {
                voided++;
            } else {
                refused++;
            }
        }
        result.set(new BulkVoidResult(voided, refused));
    }
}
