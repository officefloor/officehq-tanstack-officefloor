package net.officefloor.hq.app.claim;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Settles the selected claims in list order (people in the order they were added, newest claim first
 * within a person), each by exactly the rules of settling one claim and audited as a single settle
 * would be. A claim that goes to PENDING counts as settled; a selected claim that cannot be settled is
 * left as it is and counted as refused; so is a selected id that is not a claim on the sheet.
 */
public class BulkSettleClaims {
    public void settle(@PathVariable("sheetId") String sheetId, @RequestBody BulkSettleRequest request,
            Claims claims, Sheets sheets, People people, Rates rates, Periods periods, Settings settings,
            ClaimNumbering numbering, Rollup rollup, Audit audit, ClaimLog log, Watches watches,
            ObjectResponse<BulkSettleResponse> response) {
        Set<String> selected = new HashSet<>(request.claimIds() == null ? List.of() : request.claimIds());
        ValidClaimStatus settle = new ValidClaimStatus(ClaimStatus.SETTLED, request.user(),
                ValidClaimStatus.Decision.NONE, null);
        List<Claim> all = claims.list(sheetId);
        int settled = 0;
        for (Person person : people.list(sheetId)) {
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId() || !selected.contains(String.valueOf(claim.getId()))) {
                    continue;
                }
                RollupFigures before = rollup.figures(sheetId);
                ClaimNumber number;
                try {
                    number = ApplyClaimStatus.change(settle, claim, sheets, people, rates, periods, settings, numbering);
                } catch (ClaimStatusRefusedException refused) {
                    continue;
                }
                claims.save(claim);
                AuditClaimStatus.record(settle, claim, number, people, audit, before, rollup, log, watches);
                settled++;
            }
        }
        response.send(new BulkSettleResponse(settled, selected.size() - settled));
    }
}
