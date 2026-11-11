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
 * Holds each selected person, in list order (the order they were added), exactly as the row's hold
 * would (see {@link HoldPerson}). Each is audited as a single hold is: its PERSON_HELD line, then the
 * figures it moved. A viewer, or anyone on an archived sheet, has every selected person refused: none
 * is held and nothing is written. A to-date before the from-date refuses them all.
 */
public class HoldSelectedPeople {
    public void hold(@Val Sheet sheet, @Val BulkHoldRequest request, PersonRepository people,
            ClaimRepository claims, RateRepository rates, SettingsRepository settings, RoleRepository roles,
            HttpServletRequest http, Audit audit, Out<BulkHoldResponse> result)
            throws InvalidPersonException {
        String user = Audit.user(http);
        if (sheet.archived() || Role.VIEWER.equals(roles.roleOf(sheet.id(), user))) {
            result.set(new BulkHoldResponse(0));
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
        LocalDate to = request.to().isEmpty() ? null : LocalDate.parse(request.to());
        if (to != null && to.isBefore(from)) {
            throw new InvalidPersonException("A hold's to-date is not before its from-date");
        }
        Rollup before = rollup(sheet, people, claims, rates, settings);
        int held = 0;
        for (Person person : people.findAll(sheet.id())) {
            if (!selected.contains(person.getId())) continue;
            Hold hold = new Hold(from, to);
            person.getHolds().add(hold);
            people.addHold(person, hold);
            held++;
            audit.record("PERSON_HELD sheet=" + sheet.id() + " by=" + user
                    + " name=" + person.getName() + " from=" + hold.from());
            Rollup after = rollup(sheet, people, claims, rates, settings);
            after.changesSince(before, sheet.id(), user).forEach(audit::record);
            before = after;
        }
        result.set(new BulkHoldResponse(held));
    }

    private static Rollup rollup(Sheet sheet, PersonRepository people, ClaimRepository claims, RateRepository rates,
            SettingsRepository settings) {
        return Rollup.of(sheet.id(), sheet.today(), people, claims, rates, settings.find(sheet.id()));
    }
}
