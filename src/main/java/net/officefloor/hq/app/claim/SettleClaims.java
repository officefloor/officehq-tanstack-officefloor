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
import net.officefloor.hq.app.watch.WatchStore;
import net.officefloor.plugin.variable.Val;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Settles each selected claim, in list order: its people in the order they were added, a person's
 *  claims newest first. Each goes through the very steps of a single settle by the user (period,
 *  clerk limit, opened-by and status checks, then store, log and audit), so a clerk's settle above
 *  the clerk limit sends it to PENDING, which counts as settled. A claim that is not on the sheet,
 *  or that a single settle would refuse, is refused and left as it is. */
public class SettleClaims {
    public void settle(@PathVariable("sheetId") String sheetId, @Val BulkSettleInput input, ClaimStore store,
            PersonStore people, PeriodStore periods, SettingStore settings, SheetStore sheets, RoleStore roles,
            RateStore rates, ClaimLogStore log, RollupService rollups, Audit audit, WatchStore watches,
            Out<BulkSettleResult> result) {
        String sheet = sheetId.trim();
        Map<Long, Integer> personOrder = new HashMap<>();
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
                .thenComparing(Comparator.comparingLong(Claim::getId).reversed()));
        StatusInput status = new StatusInput(ClaimStatus.SETTLED, input.user(), false, "");
        int settled = 0;
        for (Claim claim : selected) {
            try {
                new CheckClaimPeriod().check(claim, periods);
                StatusMove[] move = new StatusMove[1];
                new CheckClerkLimit().check(status, claim, roles, settings, people, rates, sheets, store, m -> move[0] = m);
                Rollup before = rollups.snapshot(sheet);
                StatusChange[] change = new StatusChange[1];
                new ApplyStatus().apply(status, move[0], claim, store, people, rates, settings, sheets, periods,
                        c -> change[0] = c);
                new SaveClaim().save(status, claim, store, log);
                new AuditClaimStatus().audit(status, move[0], claim, change[0], before, people, audit, rollups, watches);
                settled++;
            } catch (InvalidSheetException e) {
                refused++;
            }
        }
        result.set(new BulkSettleResult(settled, refused));
    }
}
