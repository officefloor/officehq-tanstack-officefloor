package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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

/** Settle the selected claims by the signed-in user, all or nothing. When any selected claim cannot be settled by the
 *  rules of settling one claim (the sheet is archived, it is not on the sheet, not allowed from its status or by this
 *  user, or dated in a closed period; a clerk's settle above the clerk limit going to PENDING is not a refusal), none
 *  is settled, every one counts as refused and nothing is written. Otherwise the bulk settle is the sheet's next batch
 *  and each is settled by it in list order (people in the order they were added, newest claim first within a person):
 *  its status log line, its CLAIM_SETTLED or CLAIM_PENDING line (with its NOTIFY lines for the person's watchers when
 *  SETTLED) and then how the sheet's figures moved over it; after them one BATCH_SETTLED line with the batch number,
 *  how many claims it settled and the frozen dues of those SETTLED added up. */
public class BulkSettleClaims {
    public void settle(@PathVariable("sheetId") String sheetId, @Val BulkSettleInput input, ClaimRepository claims,
            ClaimLogRepository log, PersonRepository people, PeriodRepository periods, RateRepository rates,
            RoleRepository roles, SettingRepository settings, SheetRepository sheets, Rollups rollups,
            WatchRepository watches, SettleBatchRepository batches, Audit audit, Out<BulkSettleResponse> result)
            throws ClaimStatusNotAllowedException {
        String sheet = sheetId.trim();
        periods.lockSheet(sheet);
        ClaimStatusInput settle = new ClaimStatusInput(input.user(), "settle", ClaimStatus.SETTLED, null);
        BulkSettleResponse refused = new BulkSettleResponse(0, input.claimIds().size());
        if (sheets.archived(sheet)) {
            result.set(refused);
            return;
        }
        List<Claim> selected = new ArrayList<>();
        for (Person person : people.list(sheet)) {
            claims.listForPerson(sheet, person.id()).stream()
                    .filter(c -> input.claimIds().contains(c.id())).forEach(selected::add);
        }
        if (selected.size() != input.claimIds().size()) {
            result.set(refused);
            return;
        }
        for (Claim claim : selected) {
            if (periods.closedOn(sheet, claim.dueBy()) != null) {
                result.set(refused);
                return;
            }
            try {
                ClaimStatusChange.check(settle, claim, claims, people, rates, roles, settings, sheets, log);
            } catch (ClaimStatusNotAllowedException notAllowed) {
                result.set(refused);
                return;
            }
        }
        int batch = batches.start(sheet);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Claim claim : selected) {
            Rollup before = rollups.take(sheet);
            // checked above with the sheet locked; a refusal now throws and the transaction rolls back
            ClaimResponse saved = ClaimStatusChange.save(settle, claim, claims, people, rates, roles, settings,
                    sheets, log);
            batches.settledBy(sheet, claim.id(), batch);
            ClaimStatusChange.audit(settle, claim, saved, people, watches, audit);
            rollups.record(sheet, input.user(), before);
            if ("SETTLED".equals(saved.status()) && saved.settledDue() != null) {
                total = total.add(new BigDecimal(saved.settledDue()));
            }
        }
        audit.record("BATCH_SETTLED sheet=" + sheet + " by=" + input.user() + " batch=" + batch
                + " count=" + selected.size() + " total=" + total.setScale(2, RoundingMode.HALF_UP));
        result.set(new BulkSettleResponse(selected.size(), 0));
    }
}
