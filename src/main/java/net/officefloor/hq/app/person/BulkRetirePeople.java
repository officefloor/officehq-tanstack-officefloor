package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
 * Retires each selected person, in list order (by position), from the date exactly as the row's retire would, each
 * with its own PERSON_RETIRED and rollup lines. A bad date, or a selected person not on the sheet, refuses the whole
 * action before anything is written. A viewer retires no one: it reports 0 and writes nothing.
 */
public class BulkRetirePeople {

    public void retire(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody BulkRetireRequest request, People people, SheetClock clock, Audit audit, Rollup rollup,
            Roles roles, ObjectResponse<BulkRetireResponse> response) throws InvalidPersonException {
        String date = request == null || request.from() == null ? "" : request.from().trim();
        LocalDate from;
        try {
            from = date.isEmpty() ? clock.today(sheet) : LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("Not a valid date: " + date);
        }
        Set<Long> selected = new HashSet<>(request.people() == null ? List.of() : request.people());
        List<Person> everyone = people.list(sheet);
        List<Person> chosen = everyone.stream().filter((p) -> selected.contains(p.id())).toList();
        if (chosen.size() != selected.size()) {
            throw new InvalidPersonException("Every selected person must be on the sheet");
        }
        if (Role.VIEWER.equals(roles.of(sheet, user))) {
            response.send(new BulkRetireResponse(0, everyone));
            return;
        }
        for (Person person : chosen) {
            RetirePerson.apply(sheet, user, person, from, people, audit, rollup);
        }
        response.send(new BulkRetireResponse(chosen.size(), people.list(sheet)));
    }
}
