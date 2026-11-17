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
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Voids each selected claim, in list order (people in list order, see PersonRepository#list; a person's claims newest
 * first), exactly as the single void does: judged by ApplyClaimStatus (with the void reason chosen), stored by
 * SaveClaimStatus and written by RecordClaimStatusChanged (its CLAIM_VOIDED, then the figures it moved). Each the single
 * void would refuse, or not on the sheet, is left as it is and counted refused.
 */
public class VoidClaims {
    public void voidAll(@PathVariable("sheetId") String sheetId, @Val BulkVoidRequest request, ClaimRepository claims,
            PersonRepository people, RateRepository rates, PeriodRepository periods, SettingRepository settings,
            RoleRepository roles, SheetRepository sheets, ClaimLog log, VoidReasonRepository voidReasons,
            WatchRepository watches, Rollup rollup, Audit audit, Out<BulkVoidResult> result) {
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
        ClaimStatusChange change = new ClaimStatusChange(ClaimAction.VOID, ClaimStatus.VOID, request.by(),
                request.reason());
        int voided = 0;
        for (Claim claim : chosen) {
            RollupBefore before = rollup.take(sheet);
            Claim[] changed = new Claim[1];
            Claim[] saved = new Claim[1];
            ClaimNumber[] number = new ClaimNumber[1];
            PreviousDue[] previous = new PreviousDue[1];
            try {
                ApplyClaimStatus.applyJudged(claim, claim, change, people, rates, periods, settings, roles, sheets,
                        claims, log, voidReasons, (c) -> changed[0] = c, (n) -> number[0] = n,
                        (p) -> previous[0] = p);
                new SaveClaimStatus().save(changed[0], change, claims, periods, settings, sheets, log, voidReasons,
                        (c) -> saved[0] = c, (n) -> number[0] = n);
            } catch (InvalidClaimStatusException e) {
                continue;
            }
            voided++;
            new RecordClaimStatusChanged().record(saved[0], change, number[0], previous[0], people, watches, before,
                    rollup, audit);
        }
        result.set(new BulkVoidResult(voided, selected.size() - voided));
    }
}
