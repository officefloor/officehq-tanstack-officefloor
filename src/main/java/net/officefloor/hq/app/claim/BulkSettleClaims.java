package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Settle each selected claim by the signed-in user, in list order (people in the order they were added, newest
 *  claim first within a person), by exactly the rules of settling one claim: each that goes through (SETTLED, or
 *  PENDING for a clerk's settle above the clerk limit) gets its status log line, its CLAIM_SETTLED or CLAIM_PENDING
 *  line (with its NOTIFY lines for the person's watchers when SETTLED) and then how the sheet's figures moved over it. A selected claim that cannot be settled (the sheet is archived, not on the sheet,
 *  not allowed from its status or by this user, or dated in a closed period) is left as it is and counted as
 *  refused. */
public class BulkSettleClaims {
    public void settle(@PathVariable("sheetId") String sheetId, @Val BulkSettleInput input, ClaimRepository claims,
            ClaimLogRepository log, PersonRepository people, PeriodRepository periods, RateRepository rates,
            RoleRepository roles, SettingRepository settings, SheetRepository sheets, Rollups rollups,
            WatchRepository watches, Audit audit, Out<BulkSettleResponse> result) {
        String sheet = sheetId.trim();
        periods.lockSheet(sheet);
        ClaimStatusInput settle = new ClaimStatusInput(input.user(), "settle", ClaimStatus.SETTLED, null);
        int settled = 0;
        if (sheets.archived(sheet)) {
            result.set(new BulkSettleResponse(0, input.claimIds().size()));
            return;
        }
        for (Person person : people.list(sheet)) {
            List<Claim> theirs = claims.listForPerson(sheet, person.id()).stream()
                    .filter(c -> input.claimIds().contains(c.id())).toList();
            for (Claim claim : theirs) {
                if (periods.closedOn(sheet, claim.dueBy()) != null) {
                    continue;
                }
                Rollup before = rollups.take(sheet);
                ClaimResponse saved;
                try {
                    saved = ClaimStatusChange.save(settle, claim, claims, people, rates, roles, settings, sheets, log);
                } catch (ClaimStatusNotAllowedException refused) {
                    continue;
                }
                ClaimStatusChange.audit(settle, claim, saved, people, watches, audit);
                rollups.record(sheet, input.user(), before);
                settled++;
            }
        }
        result.set(new BulkSettleResponse(settled, input.claimIds().size() - settled));
    }
}
