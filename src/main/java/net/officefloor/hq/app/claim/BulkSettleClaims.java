package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
 * would be. A claim that goes to PENDING counts as settled. It is all-or-nothing: when any selected claim cannot be
 * settled (or a selected id is not a claim on the sheet), none is settled, every one counts as refused and nothing is
 * written. A bulk settle that settles at least one claim is the sheet's next batch, and each claim it settled keeps
 * that batch's number; after the claims' own lines it audits BATCH_SETTLED with the batch's count and the total of
 * the frozen figures of those SETTLED.
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
        // First every selected claim is judged by the rules of a single settle; a refusal of any refuses them all.
        List<Claim> changed = new ArrayList<>();
        List<ClaimNumber> numbers = new ArrayList<>();
        for (Person person : people.list(sheetId)) {
            for (Claim claim : all) {
                if (claim.getPersonId() != person.getId() || !selected.contains(String.valueOf(claim.getId()))) {
                    continue;
                }
                try {
                    numbers.add(ApplyClaimStatus.change(settle, claim, sheets, people, rates, periods, settings, numbering, log));
                } catch (ClaimStatusRefusedException refused) {
                    response.send(new BulkSettleResponse(0, selected.size()));
                    return;
                }
                changed.add(claim);
            }
        }
        if (changed.size() != selected.size()) {
            // A selected id that is not a claim on the sheet is refused, so the whole bulk settle is.
            response.send(new BulkSettleResponse(0, selected.size()));
            return;
        }
        List<Claim> settled = new ArrayList<>();
        for (int i = 0; i < changed.size(); i++) {
            Claim claim = changed.get(i);
            RollupFigures before = rollup.figures(sheetId);
            claims.save(claim);
            AuditClaimStatus.record(settle, claim, numbers.get(i), people, audit, before, rollup, log, watches);
            settled.add(claim);
        }
        if (!settled.isEmpty()) {
            int batch = sheets.nextSettleBatch(sheetId);
            BigDecimal total = new BigDecimal("0.00");
            for (Claim claim : settled) {
                claim.setSettleBatch(batch);
                claims.settleBatch(claim);
                if (claim.getStatus() == ClaimStatus.SETTLED && claim.getSettledDue() != null) {
                    total = total.add(claim.getSettledDue());
                }
            }
            audit.recordOnCommit("BATCH_SETTLED sheet=" + sheetId + " by=" + request.user() + " batch=" + batch
                    + " count=" + settled.size() + " total=" + total.setScale(2, RoundingMode.HALF_UP).toPlainString());
        }
        response.send(new BulkSettleResponse(settled.size(), selected.size() - settled.size()));
    }
}
