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
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Voids the selected claims in list order (people in the order they were added, newest claim first
 *  within a person), each by exactly the rules of {@link VoidClaim} and recorded as it would record it
 *  (its status log line, its CLAIM_VOIDED line, then the rollup lines it moved). A claim that cannot be
 *  voided (the user's role, its status, a closed period, a missing void reason, or it is not on the sheet)
 *  is left as it is and counted as refused; every one is for a viewer or on an archived sheet. */
public class BulkVoidClaims {
    public void voidAll(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @RequestBody BulkVoidRequest request, ClaimRepository claims, PeriodRepository periods,
            PersonRepository people, RateRepository rates, SheetRepository sheets,
            SettingRepository settings, RoleRepository roles, ClaimLogRepository log,
            VoidReasonRepository reasons, Audit audit, Out<BulkVoidResult> result) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        Set<Long> selected = new HashSet<>(request == null || request.claimIds() == null
                ? List.of() : request.claimIds());
        selected.remove(null);
        String reason = request == null ? null : request.reason();

        // The selected claims of the sheet, in list order.
        List<Claim> all = claims.list(id);
        List<Claim> ordered = new ArrayList<>();
        for (Person p : people.list(id)) {
            for (Claim c : all) {
                if (c.getPersonId() == p.getId() && selected.contains(c.getId())) ordered.add(c);
            }
        }

        boolean refusedAll = sheets.isArchived(id) || Role.VIEWER.equals(roles.roleOf(id, user));
        VoidClaim rules = new VoidClaim();
        int voided = 0;
        Rollup before = Rollup.of(id, people, claims, rates, sheets, settings);
        for (Claim claim : ordered) {
            if (refusedAll) continue;
            try {
                rules.move(user, reason, claim, periods, roles, settings, log, sheets, reasons);
            } catch (ClaimStatusException | InvalidClaimException | PeriodClosedException refused) {
                continue;
            }
            claims.update(claim);
            log.record(claim, user);
            voided++;
            audit.record(RecordClaimStatus.line(ClaimStatus.VOID.event(), user, claim, people)
                    + (claim.isReversal() ? RecordClaimStatus.reversed(claim) : "")
                    + (claim.getVoidReason() != null ? " reason=" + claim.getVoidReason() : ""));
            Rollup after = Rollup.of(id, people, claims, rates, sheets, settings);
            for (String line : after.changesSince(before, id, user)) {
                audit.record(line);
            }
            before = after;
        }
        result.set(new BulkVoidResult(voided, selected.size() - voided));
    }
}
