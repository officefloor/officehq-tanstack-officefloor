package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Floor;
import net.officefloor.hq.app.setting.LatePct;
import net.officefloor.hq.app.setting.PolicyDate;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Opens each selected claim in list order (people in the order they were added, each person's claims
 *  newest first), so their numbers follow that order. Each is opened by the same rules, and writes the same
 *  audit lines, as opening it alone; one those rules refuse (or not on the sheet) is left as it is. */
public class BulkOpenClaims {
    public void open(@PathVariable("sheetId") String sheetId, @Val BulkOpenRequest request,
            @RequestParam(value = "user", required = false) String user, ClaimRepository claims,
            PersonRepository people, PeriodRepository periods, RateRepository rates, Floor floor, LatePct latePct,
            PolicyDate policyDate, ClaimNumbering numbering, RoleRepository roles, Rollup rollup, Audit audit, Out<BulkOpenResult> resultOut) {
        Set<Long> selected = new HashSet<>(request.claimIds());
        List<Claim> sheetClaims = claims.findBySheet(sheetId);
        List<Claim> ordered = new ArrayList<>();
        for (Person person : people.findBySheet(sheetId)) {
            for (Claim claim : sheetClaims) {
                if (claim.getPersonId() == person.getId() && selected.contains(claim.getId())) ordered.add(claim);
            }
        }
        int opened = 0;
        for (Claim claim : ordered) {
            Rollup.Snapshot before = rollup.take(sheetId);
            ClaimNumber[] number = new ClaimNumber[1];
            try {
                new CheckClaimActionOpen().check(claim, periods);
                new ApplyClaimAction().apply(ClaimAction.OPEN, claim, numbering, people, rates, floor, latePct, policyDate,
                        roles, n -> number[0] = n, user, null);
            } catch (InvalidClaimStatusException refused) {
                continue;
            }
            claims.save(claim);
            new AuditClaimStatus().audit(ClaimAction.OPEN, claim, number[0], user, null, people, audit);
            rollup.changes(sheetId, user, before, rollup.take(sheetId)).forEach(audit::record);
            opened++;
        }
        resultOut.set(new BulkOpenResult(opened, selected.size() - opened));
    }
}
