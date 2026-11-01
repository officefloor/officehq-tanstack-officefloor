package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a person per checked line, in order, as the person form's BuildPerson adds one. */
public class AddImportedPeople {
    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val ImportedPeople imported, People people, Audit audit, Rollup rollup,
            ObjectResponse<ImportResponse> response) throws DuplicatePersonNameException {
        for (PersonRequest request : imported.lines()) {
            Rollup.Snapshot before = rollup.take(sheet);
            Person added = people.add(sheet, request.name(), request.grade(), request.termsDays(), request.contactOrNull());
            audit.record("PERSON_ADDED sheet=" + sheet + " by=" + user + " name=" + added.name() + " grade="
                    + added.grade() + (added.terms() == null ? "" : " terms=" + added.terms())
                    + (added.contact() == null ? "" : " contact=" + added.contact()));
            rollup.record(sheet, user, before);
        }
        response.send(new ImportResponse(imported.lines().size(), people.list(sheet)));
    }
}
