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
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Settles each selected claim, in list order (people in the order they were added, a person's claims newest first),
 * through the very steps of the single settle: ApplyClaimStatus (its rules, a clerk's settle above the clerk limit
 * making it PENDING), SaveClaimStatus (its status log line) and RecordClaimStatusChanged (CLAIM_SETTLED or
 * CLAIM_PENDING and the figures it moved). A claim made PENDING counts as settled. Each that the single settle would
 * refuse, or is not on the sheet, is left as it is and counted refused.
 */
public class SettleClaims {
    public void settle(@PathVariable("sheetId") String sheetId, @Val BulkSettleRequest request, ClaimRepository claims,
            PersonRepository people, RateRepository rates, PeriodRepository periods, SettingRepository settings,
            RoleRepository roles, SheetRepository sheets, WatchRepository watches, Rollup rollup, Audit audit, ClaimLog log,
            Out<BulkSettleResult> result) {
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
        ClaimStatusChange change = new ClaimStatusChange(ClaimAction.SETTLE, ClaimStatus.SETTLED, request.by(), "");
        ApplyClaimStatus apply = new ApplyClaimStatus();
        SaveClaimStatus save = new SaveClaimStatus();
        RecordClaimStatusChanged record = new RecordClaimStatusChanged();
        int settled = 0;
        for (Claim listedClaim : chosen) {
            Claim claim = claims.find(sheet, listedClaim.id());
            if (claim == null) {
                continue;
            }
            RollupBefore before = rollup.take(sheet);
            Claim[] changed = new Claim[1];
            Claim[] stored = new Claim[1];
            ClaimNumber[] number = new ClaimNumber[1];
            PreviousDue[] previous = new PreviousDue[1];
            try {
                apply.apply(claim, change, people, rates, periods, settings, roles, sheets, claims,
                        (c) -> changed[0] = c, (n) -> number[0] = n, (p) -> previous[0] = p);
                save.save(changed[0], change, claims, periods, settings, sheets, log, (c) -> stored[0] = c,
                        (n) -> number[0] = n);
            } catch (InvalidClaimStatusException refused) {
                continue;
            }
            settled++;
            record.record(stored[0], change, number[0], previous[0], people, watches, before, rollup, audit);
        }
        result.set(new BulkSettleResult(settled, selected.size() - settled));
    }
}
