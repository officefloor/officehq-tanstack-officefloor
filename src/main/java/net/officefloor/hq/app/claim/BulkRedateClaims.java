package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import net.officefloor.hq.app.pool.PoolRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Re-dates the selected claims to one date in list order (people in the order they were added, newest
 *  claim first within a person), each by exactly the rules of {@link RedateClaim#redate} and recorded as
 *  it would record it (its CLAIM_REDATED line, then the rollup lines it moved). A claim that cannot be
 *  re-dated (the user's role, its status, a closed period, the date, the person's retirement, or it is not
 *  on the sheet) is left as it is and counted as refused; every one is on an archived sheet. */
public class BulkRedateClaims {
    public void redate(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @RequestBody BulkRedateRequest request, ClaimRepository claims, PeriodRepository periods,
            PersonRepository people, RateRepository rates, SheetRepository sheets,
            SettingRepository settings, PoolRepository pools, RoleRepository roles, Audit audit,
            Out<BulkRedateResult> result) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        Set<Long> selected = new HashSet<>(request == null || request.claimIds() == null
                ? List.of() : request.claimIds());
        selected.remove(null);
        String date = request == null ? null : request.date();

        // The selected claims of the sheet, in list order.
        List<Claim> all = claims.list(id);
        List<Claim> ordered = new ArrayList<>();
        for (Person p : people.list(id)) {
            for (Claim c : all) {
                if (c.getPersonId() == p.getId() && selected.contains(c.getId())) ordered.add(c);
            }
        }

        boolean archived = sheets.isArchived(id);
        int redated = 0;
        Rollup before = Rollup.of(id, people, claims, rates, sheets, settings, pools);
        for (Claim claim : ordered) {
            if (archived) continue;
            Redating redating;
            try {
                redating = RedateClaim.redate(user, claim, date, people, roles, periods);
            } catch (ClaimStatusException | InvalidClaimException | PeriodClosedException refused) {
                continue;
            }
            claims.redate(claim);
            redated++;
            audit.record(RecordClaimStatus.line("CLAIM_REDATED", user, claim, people)
                    + " old=" + redating.oldDate() + " new=" + redating.newDate());
            Rollup after = Rollup.of(id, people, claims, rates, sheets, settings, pools);
            for (String line : after.changesSince(before, id, user)) {
                audit.record(line);
            }
            before = after;
        }
        result.set(new BulkRedateResult(redated, selected.size() - redated));
    }
}
