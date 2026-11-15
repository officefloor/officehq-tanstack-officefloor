package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.WatchRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Settles the selected claims in list order (people in the order they were added, newest claim first
 *  within a person), each by exactly the rules of {@link SettleClaim} and recorded as it would record
 *  it (its status log line, its CLAIM_SETTLED or CLAIM_PENDING line, a settled one's NOTIFY lines, then the rollup lines it moved).
 *  A claim gone to PENDING counts as settled. All or nothing: if any selected claim cannot be settled
 *  (its status does not allow it, the user opened it, its due-by is in a closed period, it is not on the
 *  sheet, or the sheet is archived) none is settled, every one counts as refused and nothing is written.
 *  Otherwise the bulk settle is the sheet's next batch, each claim keeps that batch number, and after the
 *  claims' own lines one BATCH_SETTLED line gives the batch, its count and the SETTLED ones' frozen dues added up. */
public class BulkSettleClaims {
    public void settle(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @RequestBody BulkSettleRequest request, ClaimRepository claims, PeriodRepository periods,
            PersonRepository people, RateRepository rates, SheetRepository sheets,
            SettingRepository settings, RoleRepository roles, ClaimLogRepository log, WatchRepository watches, Audit audit,
            Out<BulkSettleResult> result) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        Set<Long> selected = new HashSet<>(request == null || request.claimIds() == null
                ? List.of() : request.claimIds());
        selected.remove(null);

        // The selected claims of the sheet, in list order.
        List<Claim> all = claims.list(id);
        List<Claim> ordered = new ArrayList<>();
        for (Person p : people.list(id)) {
            for (Claim c : all) {
                if (c.getPersonId() == p.getId() && selected.contains(c.getId())) ordered.add(c);
            }
        }

        // All or nothing: every selected claim must settle by the rules of settling one (going to PENDING
        // is not a refusal), or none is settled and every one counts as refused.
        boolean refused = sheets.isArchived(id) || ordered.size() != selected.size()
                || Role.VIEWER.equals(roles.roleOf(id, user));
        for (Claim claim : ordered) {
            if (refused) break;
            try {
                SettleClaim.settle(user, claim, people, rates, periods, roles, settings, sheets);
            } catch (ClaimStatusException | PeriodClosedException e) {
                refused = true;
            }
        }
        if (refused || ordered.isEmpty()) {
            result.set(new BulkSettleResult(0, selected.size()));
            return;
        }

        int batch = sheets.nextSettleBatch(id);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        Rollup before = Rollup.of(id, people, claims, rates, sheets, settings);
        for (Claim claim : ordered) {
            claim.setSettleBatch(batch);
            ClaimNumbers.settled(claim, claims, periods, settings, sheets);
            claims.update(claim);
            log.record(claim, user);
            audit.record(RecordClaimStatus.line(ClaimStatus.valueOf(claim.getStatus()).event(), user, claim, people)
                    + (ClaimStatus.SETTLED.name().equals(claim.getStatus()) ? RecordClaimStatus.due(claim) : ""));
            if (ClaimStatus.SETTLED.name().equals(claim.getStatus())) {
                if (claim.getSettledDue() != null) total = total.add(new BigDecimal(claim.getSettledDue()));
                Notify.watchers(user, claim, people, watches, audit);
            }
            Rollup after = Rollup.of(id, people, claims, rates, sheets, settings);
            for (String line : after.changesSince(before, id, user)) {
                audit.record(line);
            }
            before = after;
        }
        audit.record("BATCH_SETTLED sheet=" + id + " by=" + user + " batch=" + batch
                + " count=" + ordered.size() + " total=" + total.toPlainString());
        result.set(new BulkSettleResult(ordered.size(), 0));
    }
}
