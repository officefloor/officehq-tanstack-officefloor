package net.officefloor.hq.app.claim;

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
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Void each selected claim, in list order (people in the order they were added, newest claim first within a person,
 *  as the sheet was when the void began), by exactly the rules of voiding one claim: each that goes through gets its
 *  status log line, its void reason recorded, its CLAIM_VOIDED line and then how the sheet's figures moved over it. A
 *  selected claim that cannot be voided (not on the sheet, dated in a closed period, not allowed from its status or by
 *  this user, or without a reason from the sheet's list while it has any) is left as it is and counted as refused. */
public class BulkVoidClaims {
    public void voidClaims(@PathVariable("sheetId") String sheetId, @Val BulkVoidInput input, ClaimRepository claims,
            ClaimLogRepository log, PersonRepository people, PeriodRepository periods, RateRepository rates,
            RoleRepository roles, SettingRepository settings, SheetRepository sheets, VoidReasonRepository reasons,
            WatchRepository watches, Rollups rollups, Audit audit, Out<BulkVoidResponse> result) {
        String sheet = sheetId.trim();
        periods.lockSheet(sheet);
        if (sheets.archived(sheet)) {
            result.set(new BulkVoidResponse(0, input.claimIds().size()));
            return;
        }
        if (reasons.any(sheet) && (input.reason() == null || !reasons.exists(sheet, input.reason()))) {
            result.set(new BulkVoidResponse(0, input.claimIds().size()));
            return;
        }
        List<Claim> selected = new ArrayList<>();
        for (Person person : people.list(sheet)) {
            claims.listForPerson(sheet, person.id()).stream()
                    .filter(c -> input.claimIds().contains(c.id())).forEach(selected::add);
        }
        ClaimStatusInput voiding = new ClaimStatusInput(input.user(), "void", ClaimStatus.VOID, input.reason());
        int voided = 0;
        for (Claim listed : selected) {
            Claim claim = claims.find(sheet, listed.id());
            if (claim == null || periods.closedOn(sheet, claim.dueBy()) != null) {
                continue;
            }
            Rollup before = rollups.take(sheet);
            ClaimResponse saved;
            try {
                saved = ClaimStatusChange.save(voiding, claim, claims, people, rates, roles, settings, sheets, log);
            } catch (ClaimStatusNotAllowedException refused) {
                continue;
            }
            if (input.reason() != null) {
                reasons.voided(sheet, claim.id(), input.reason());
            }
            ClaimStatusChange.audit(voiding, claim, saved, people, watches, audit);
            rollups.record(sheet, input.user(), before);
            voided++;
        }
        result.set(new BulkVoidResponse(voided, input.claimIds().size() - voided));
    }
}
