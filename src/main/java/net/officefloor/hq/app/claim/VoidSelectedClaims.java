package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Voids the selected claims with one reason in list order (people by their position in the list, each
 * person's claims newest first, as listed before any is voided) by the rules of voiding one claim, as
 * the signed-in user. A claim that cannot be voided (its status does not allow it, a role the
 * permission matrix refuses, a SETTLED claim voided by anyone but a steward or too long after its
 * settlement, its due-by inside a closed period, a reason missing or not on the sheet's list) is
 * refused and left as it is. Each claim voided is logged and audited as a single void is: its
 * CLAIM_VOIDED line, then the figures it moved. A viewer, or anyone on an archived sheet, voids none:
 * every selected claim is refused.
 */
public class VoidSelectedClaims {
    public void voidClaims(@Val Sheet sheet, @Val BulkVoidRequest request, ClaimRepository claims,
            PersonRepository people, PeriodRepository periods, SheetRepository sheets, RateRepository rates,
            RoleRepository roles, SettingsRepository settings, ClaimLogRepository log, VoidReasonRepository reasons,
            HttpServletRequest http, Audit audit, Out<BulkVoidResponse> result) {
        String user = Audit.user(http);
        Set<Long> selected = new HashSet<>();
        for (String id : request.claimIds()) {
            try {
                selected.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException | NullPointerException e) {
                // not an id: no claim on the sheet, so refused below
            }
        }
        int requested = new HashSet<>(request.claimIds()).size();
        if (sheet.archived() || Role.VIEWER.equals(roles.roleOf(sheet.id(), user))) {
            result.set(new BulkVoidResponse(0, requested)); // nothing may be voided: every claim refused
            return;
        }
        List<Claim> listed = new ArrayList<>();
        for (Person person : people.findAll(sheet.id())) {
            for (Claim claim : claims.findByPerson(sheet.id(), person.getId())) {
                if (selected.contains(claim.getId())) listed.add(claim);
            }
        }
        int voided = 0;
        Rollup before = rollup(sheet, sheets, people, claims, rates, settings);
        for (Claim claim : listed) {
            if (periods.closedOn(sheet.id(), claim.getDueBy()) != null) continue;
            try {
                VoidClaim.voidClaim(sheet, claim, request.reason(), user, roles, settings, log, reasons);
            } catch (InvalidClaimStatusException | InvalidClaimException e) {
                continue;
            }
            claims.save(claim);
            log.record(claim, user, sheet.today(), LogClaimStatus.due(sheet, claim, people, rates, settings));
            voided++;
            audit.record(AuditClaimStatus.line(sheet, claim, user, people, log));
            Rollup after = rollup(sheet, sheets, people, claims, rates, settings);
            after.changesSince(before, sheet.id(), user).forEach(audit::record);
            before = after;
        }
        result.set(new BulkVoidResponse(voided, requested - voided));
    }

    private static Rollup rollup(Sheet sheet, SheetRepository sheets, PersonRepository people, ClaimRepository claims,
            RateRepository rates, SettingsRepository settings) {
        return Rollup.of(sheet.id(), sheets.today(sheet.id()), people, claims, rates, settings.find(sheet.id()));
    }
}
