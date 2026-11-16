package net.officefloor.hq.app.claim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Re-dates each selected claim, in list order (people in list order, see PersonRepository#list; a person's claims
 * newest first), by the checks of the single re-date (ApplyRedate: who may, which statuses, closed periods,
 * retirement), stored as SaveRedate stores it and written as RecordClaimRedated writes it (CLAIM_REDATED and the
 * figures it moved). Each that the re-date refuses, or is not on the sheet, is left as it is and counted refused.
 */
public class RedateClaims {
    public void redate(@PathVariable("sheetId") String sheetId, @Val BulkRedating request, ClaimRepository claims,
            PersonRepository people, RoleRepository roles, PeriodRepository periods, Rollup rollup, Audit audit,
            Out<BulkRedateResult> result) {
        String sheet = sheetId.trim();
        Set<Long> selected = new HashSet<>(request.claimIds());
        List<Person> listed = people.list(sheet);
        Map<Long, Integer> personOrder = new HashMap<>();
        for (int i = 0; i < listed.size(); i++) {
            personOrder.put(listed.get(i).id(), i);
        }
        List<Claim> chosen = claims.list(sheet).stream().filter((c) -> selected.contains(c.id()))
                .sorted((a, b) -> Integer.compare(personOrder.getOrDefault(a.personId(), Integer.MAX_VALUE),
                        personOrder.getOrDefault(b.personId(), Integer.MAX_VALUE)))
                .toList();
        SaveRedate save = new SaveRedate();
        RecordClaimRedated record = new RecordClaimRedated();
        int redated = 0;
        for (Claim listedClaim : chosen) {
            Claim claim = claims.find(sheet, listedClaim.id());
            if (claim == null) {
                continue;
            }
            RollupBefore before = rollup.take(sheet);
            Claim[] stored = new Claim[1];
            ClaimRedating redating = new ClaimRedating(request.redating().date(), request.redating().by(), claim.date());
            try {
                Claim changed = ApplyRedate.check(claim, request.redating(), roles, people, periods);
                save.save(changed, redating, claims, (c) -> stored[0] = c);
            } catch (InvalidClaimStatusException refused) {
                continue;
            }
            redated++;
            record.record(stored[0], redating, before, people, rollup, audit);
        }
        result.set(new BulkRedateResult(redated, selected.size() - redated));
    }
}
