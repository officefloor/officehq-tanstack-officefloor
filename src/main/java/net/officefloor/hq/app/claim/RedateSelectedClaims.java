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
import net.officefloor.hq.app.person.RetiredPersonException;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.role.StewardOnlyException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Re-dates the selected claims to one date in list order (people by their position in the list, each
 * person's claims newest first, as listed before any is re-dated) by the rules of re-dating one claim,
 * as the signed-in user. A claim that cannot be re-dated (anyone but a steward, a claim not OPEN or
 * PENDING, its due-by inside a closed period, the new date after the person's retirement) is refused
 * and left as it is. Each claim re-dated is audited as a single re-dating is: its CLAIM_REDATED line,
 * then the figures it moved. A viewer, or anyone on an archived sheet, re-dates none: every selected
 * claim is refused.
 */
public class RedateSelectedClaims {
    public void redate(@Val Sheet sheet, @Val BulkRedateRequest request, @Val Redate redate, ClaimRepository claims,
            PersonRepository people, PeriodRepository periods, SheetRepository sheets, RateRepository rates,
            RoleRepository roles, SettingsRepository settings, HttpServletRequest http, Audit audit,
            Out<BulkRedateResponse> result) {
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
            result.set(new BulkRedateResponse(0, requested)); // nothing may be re-dated: every claim refused
            return;
        }
        List<Claim> listed = new ArrayList<>();
        for (Person person : people.findAll(sheet.id())) {
            for (Claim claim : claims.findByPerson(sheet.id(), person.getId())) {
                if (selected.contains(claim.getId())) listed.add(claim);
            }
        }
        int redated = 0;
        Rollup before = rollup(sheet, sheets, people, claims, rates, settings);
        for (Claim claim : listed) {
            if (periods.closedOn(sheet.id(), claim.getDueBy()) != null) continue;
            Redating redating;
            try {
                redating = RedateClaim.redate(sheet, claim, redate.date(), people, roles, user);
            } catch (StewardOnlyException | InvalidClaimStatusException | RetiredPersonException e) {
                continue;
            }
            claims.save(claim);
            redated++;
            audit.record(ClaimAudit.line("CLAIM_REDATED", sheet, user, people.find(sheet.id(), claim.getPersonId()), claim)
                    + " old=" + redating.from() + " new=" + redating.to());
            Rollup after = rollup(sheet, sheets, people, claims, rates, settings);
            after.changesSince(before, sheet.id(), user).forEach(audit::record);
            before = after;
        }
        result.set(new BulkRedateResponse(redated, requested - redated));
    }

    private static Rollup rollup(Sheet sheet, SheetRepository sheets, PersonRepository people, ClaimRepository claims,
            RateRepository rates, SettingsRepository settings) {
        return Rollup.of(sheet.id(), sheets.today(sheet.id()), people, claims, rates, settings.find(sheet.id()));
    }
}
