package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.ArrayList;
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
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Settles each selected claim, in list order (people in list order, see PersonRepository#list; a person's claims newest first),
 * through the very steps of the single settle: ApplyClaimStatus (its rules, a clerk's settle above the clerk limit
 * making it PENDING), SaveClaimStatus (its status log line) and RecordClaimStatusChanged (CLAIM_SETTLED or
 * CLAIM_PENDING and the figures it moved). A claim made PENDING counts as settled. All or nothing: when any selected
 * claim is one the single settle would refuse, or is not on the sheet, none is settled, every one counts refused and
 * nothing is written. When at least one is settled the settle is the sheet's next batch, marked on each claim it
 * settled, and after the claims' own lines BATCH_SETTLED batch=<number> count=<claims in it> total=<the frozen figures
 * of those SETTLED, added up> is written.
 */
public class SettleClaims {
    public void settle(@PathVariable("sheetId") String sheetId, @Val BulkSettleRequest request, ClaimRepository claims,
            PersonRepository people, RateRepository rates, PeriodRepository periods, SettingRepository settings,
            RoleRepository roles, SheetRepository sheets, WatchRepository watches, Rollup rollup, Audit audit, ClaimLog log,
            VoidReasonRepository voidReasons, Out<BulkSettleResult> result) {
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
        if (chosen.size() < selected.size() || !chosen.stream().allMatch((c) -> settles(c, change, apply, people,
                rates, periods, settings, roles, sheets, claims, log, voidReasons))) {
            result.set(new BulkSettleResult(0, selected.size()));
            return;
        }
        List<Long> settled = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
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
                apply.apply(claim, change, people, rates, periods, settings, roles, sheets, claims, log, voidReasons, null, null,
                        (c) -> changed[0] = c, (n) -> number[0] = n, (p) -> previous[0] = p);
                save.save(changed[0], change, claims, periods, settings, sheets, log, voidReasons, (c) -> stored[0] = c,
                        (n) -> number[0] = n);
            } catch (InvalidClaimStatusException refused) {
                continue;
            }
            settled.add(stored[0].id());
            if (stored[0].status() == ClaimStatus.SETTLED && stored[0].due() != null) {
                total = total.add(stored[0].due());
            }
            record.record(stored[0], change, number[0], previous[0], people, watches, before, rollup, audit);
        }
        if (!settled.isEmpty()) {
            int batch = claims.batch(sheet, settled, sheets.today(sheet));
            audit.record("BATCH_SETTLED sheet=" + sheet + " by=" + request.by() + " batch=" + batch + " count="
                    + settled.size() + " total=" + total.toPlainString());
        }
        result.set(new BulkSettleResult(settled.size(), selected.size() - settled.size()));
    }

    /** Whether the single settle would take the claim (to SETTLED or PENDING); checks only, writes nothing. */
    private static boolean settles(Claim claim, ClaimStatusChange change, ApplyClaimStatus apply, PersonRepository people,
            RateRepository rates, PeriodRepository periods, SettingRepository settings, RoleRepository roles,
            SheetRepository sheets, ClaimRepository claims, ClaimLog log, VoidReasonRepository voidReasons) {
        try {
            apply.apply(claim, change, people, rates, periods, settings, roles, sheets, claims, log, voidReasons, null, null,
                    (c) -> { }, (n) -> { }, (p) -> { });
            return true;
        } catch (InvalidClaimStatusException refused) {
            return false;
        }
    }
}
