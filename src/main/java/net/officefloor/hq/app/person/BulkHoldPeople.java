package net.officefloor.hq.app.person;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Gives each selected person, in list order (by position), the hold exactly as the row's hold would, each with its
 * own PERSON_HELD and rollup lines. A bad date, a to-date before the from-date, or a selected person not on the sheet
 * refuses the whole action before anything is written. A viewer holds no one: it reports 0 and writes nothing.
 */
public class BulkHoldPeople {

    public void hold(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody BulkHoldRequest request, People people, SheetClock clock, Audit audit, Rollup rollup,
            Roles roles, ObjectResponse<BulkHoldResponse> response) throws InvalidPersonException {
        Hold hold = ValidateHold.of(sheet,
                request == null ? null : new HoldRequest(request.from(), request.to()), clock);
        Set<Long> selected = new HashSet<>(request.people() == null ? List.of() : request.people());
        List<Person> everyone = people.list(sheet);
        List<Person> chosen = everyone.stream().filter((p) -> selected.contains(p.id())).toList();
        if (chosen.size() != selected.size()) {
            throw new InvalidPersonException("Every selected person must be on the sheet");
        }
        if (Role.VIEWER.equals(roles.of(sheet, user))) {
            response.send(new BulkHoldResponse(0, everyone));
            return;
        }
        for (Person person : chosen) {
            HoldPerson.apply(sheet, user, person, hold, people, audit, rollup);
        }
        response.send(new BulkHoldResponse(chosen.size(), people.list(sheet)));
    }
}
