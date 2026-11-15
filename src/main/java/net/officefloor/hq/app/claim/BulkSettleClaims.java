package net.officefloor.hq.app.claim;

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
 *  A claim gone to PENDING counts as settled. A claim that cannot be settled (its status does not
 *  allow it, the user opened it, its date is in a closed period, or it is not on the sheet) is left as
 *  it is and counted as refused. */
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

        int settled = 0;
        Rollup before = Rollup.of(id, people, claims, rates, sheets, settings);
        for (Claim claim : ordered) {
            try {
                SettleClaim.settle(user, claim, people, rates, periods, roles, settings, sheets);
            } catch (ClaimStatusException | PeriodClosedException refused) {
                continue;
            }
            claims.update(claim);
            log.record(claim, user);
            settled++;
            audit.record(RecordClaimStatus.line(ClaimStatus.valueOf(claim.getStatus()).event(), user, claim, people)
                    + (ClaimStatus.SETTLED.name().equals(claim.getStatus()) ? RecordClaimStatus.due(claim) : ""));
            if (ClaimStatus.SETTLED.name().equals(claim.getStatus())) {
                Notify.watchers(user, claim, people, watches, audit);
            }
            Rollup after = Rollup.of(id, people, claims, rates, sheets, settings);
            for (String line : after.changesSince(before, id, user)) {
                audit.record(line);
            }
            before = after;
        }
        result.set(new BulkSettleResult(settled, selected.size() - settled));
    }
}
