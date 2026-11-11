package net.officefloor.hq.app.person;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Retires each selected person, in list order (the order they were added), from the date exactly as
 * the row's retire would (see {@link RetirePerson}). Each is audited as a single retirement is: its
 * PERSON_RETIRED line, then the figures it moved. A viewer, or anyone on an archived sheet, has every
 * selected person refused: none is retired and nothing is written.
 */
public class RetireSelectedPeople {
    public void retire(@Val Sheet sheet, @Val BulkRetireRequest request, PersonRepository people,
            ClaimRepository claims, RateRepository rates, SettingsRepository settings, RoleRepository roles, HttpServletRequest http,
            Audit audit, Out<BulkRetireResponse> result) {
        String user = Audit.user(http);
        if (sheet.archived() || Role.VIEWER.equals(roles.roleOf(sheet.id(), user))) {
            result.set(new BulkRetireResponse(0));
            return;
        }
        Set<Long> selected = new HashSet<>();
        for (String id : request.personIds()) {
            try {
                selected.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException | NullPointerException e) {
                // not an id: no person on the sheet
            }
        }
        LocalDate from = request.from().isEmpty() ? sheet.today() : LocalDate.parse(request.from());
        Rollup before = rollup(sheet, people, claims, rates, settings);
        int retired = 0;
        for (Person person : people.findAll(sheet.id())) {
            if (!selected.contains(person.getId())) continue;
            person.setRetiredFrom(from);
            people.saveRetired(person);
            retired++;
            audit.record("PERSON_RETIRED sheet=" + sheet.id() + " by=" + user
                    + " name=" + person.getName() + " from=" + person.getRetiredFrom());
            Rollup after = rollup(sheet, people, claims, rates, settings);
            after.changesSince(before, sheet.id(), user).forEach(audit::record);
            before = after;
        }
        result.set(new BulkRetireResponse(retired));
    }

    private static Rollup rollup(Sheet sheet, PersonRepository people, ClaimRepository claims, RateRepository rates,
            SettingsRepository settings) {
        return Rollup.of(sheet.id(), sheet.today(), people, claims, rates, settings.find(sheet.id()));
    }
}
