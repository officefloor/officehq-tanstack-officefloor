package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Takes each checked line, in order: a line matching a person on the sheet updates them as the person form's
 * SavePerson does (grade from the sheet's today; terms and contact only when the line gives them), any other adds a
 * person as the form's BuildPerson does.
 */
public class AddImportedPeople {
    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val ImportedPeople imported, People people, SheetClock clock, Audit audit, Rollup rollup,
            ObjectResponse<ImportResponse> response)
            throws DuplicatePersonNameException, StalePersonException, InvalidPersonException {
        int added = 0;
        int updated = 0;
        for (ImportedPeople.Line line : imported.lines()) {
            PersonRequest request = line.request();
            if (line.existing() != null) {
                // Re-read so each is saved as the store holds it after the lines before it.
                Person current = people.find(sheet, line.existing().id());
                SavePerson.apply(sheet, user, current,
                        new PersonRequest(current.name(), request.grade(), null, null,
                                request.termsDays() != null ? request.terms()
                                        : current.terms() == null ? "" : current.terms().toString(),
                                request.contactOrNull() != null ? request.contact()
                                        : current.contact() == null ? "" : current.contact(), null),
                        people, clock, audit, rollup);
                updated++;
                continue;
            }
            Rollup.Snapshot before = rollup.take(sheet);
            Person person = people.add(sheet, request.name(), request.grade(), request.termsDays(), request.contactOrNull());
            audit.record("PERSON_ADDED sheet=" + sheet + " by=" + user + " name=" + person.name() + " grade="
                    + person.grade() + (person.terms() == null ? "" : " terms=" + person.terms())
                    + (person.contact() == null ? "" : " contact=" + person.contact()));
            rollup.record(sheet, user, before);
            added++;
        }
        response.send(new ImportResponse(added, updated, people.list(sheet)));
    }
}
