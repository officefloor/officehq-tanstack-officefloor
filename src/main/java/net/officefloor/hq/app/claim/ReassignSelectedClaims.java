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
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Moves the selected claims to one person in list order (people by their position in the list, each
 * person's claims newest first, as listed before any is moved) by the rules of moving one claim, as
 * the signed-in user. A claim that cannot be moved (a role the permission matrix does not allow, a
 * SETTLED or VOID claim, its due-by inside a closed period, dated after the person's retirement, the
 * person not on the sheet) is refused and left as it is. Each claim moved is audited as a single move
 * is: its CLAIM_REASSIGNED line, then the figures it moved. A viewer, or anyone on an archived sheet,
 * moves none: every selected claim is refused.
 */
public class ReassignSelectedClaims {
    public void reassign(@Val Sheet sheet, @Val BulkReassignRequest request, ClaimRepository claims,
            PersonRepository people, PeriodRepository periods, SheetRepository sheets, RateRepository rates,
            RoleRepository roles, SettingsRepository settings, HttpServletRequest http, Audit audit,
            Out<BulkReassignResponse> result) {
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
        Person to = null;
        try {
            to = people.find(sheet.id(), Long.parseLong(request.personId()));
        } catch (NumberFormatException e) {
            // not an id: not found
        }
        if (sheet.archived() || to == null || !settings.find(sheet.id()).allows("reassign", roles.roleOf(sheet.id(), user))) {
            result.set(new BulkReassignResponse(0, requested)); // nothing may be moved: every claim refused
            return;
        }
        List<Claim> listed = new ArrayList<>();
        for (Person person : people.findAll(sheet.id())) {
            for (Claim claim : claims.findByPerson(sheet.id(), person.getId())) {
                if (selected.contains(claim.getId())) listed.add(claim);
            }
        }
        int reassigned = 0;
        Rollup before = rollup(sheet, sheets, people, claims, rates, settings);
        for (Claim claim : listed) {
            if (periods.closedOn(sheet.id(), claim.getDueBy()) != null) continue;
            if (to.retiredBefore(claim.getDate())) continue;
            Person from = people.find(sheet.id(), claim.getPersonId());
            try {
                claim.reassign(to.getId());
            } catch (InvalidClaimStatusException e) {
                continue;
            }
            RenumberClaim.give(sheet, claim, claims, settings, periods);
            claims.save(claim);
            reassigned++;
            audit.record(ClaimAudit.line("CLAIM_REASSIGNED", sheet, user, to, claim)
                    + " from=" + from.getName() + " to=" + to.getName());
            Rollup after = rollup(sheet, sheets, people, claims, rates, settings);
            after.changesSince(before, sheet.id(), user).forEach(audit::record);
            before = after;
        }
        result.set(new BulkReassignResponse(reassigned, requested - reassigned));
    }

    private static Rollup rollup(Sheet sheet, SheetRepository sheets, PersonRepository people, ClaimRepository claims,
            RateRepository rates, SettingsRepository settings) {
        return Rollup.of(sheet.id(), sheets.today(sheet.id()), people, claims, rates, settings.find(sheet.id()));
    }
}
