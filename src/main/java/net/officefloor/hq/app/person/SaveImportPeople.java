package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Adds the imported people in order, each as the person form adds them (PERSON_ADDED, then the
 * figures it moved). Every line is checked against the sheet first (the name clashes with no one's
 * name or alias, the contact with no one's contact), so one refused line writes nothing.
 */
public class SaveImportPeople {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidImportPeople request, People people, Audit audit,
            Rollup rollup, ObjectResponse<ImportPeopleResponse> response) throws InvalidPersonException {
        int number = 0;
        for (PersonRequest line : request.people()) {
            number++;
            if (people.nameTaken(sheetId, line.name(), null)) {
                throw new InvalidPersonException("Person " + number + ": the name is already in use");
            }
            if (!line.contact().isEmpty() && people.contactTaken(sheetId, line.contact(), null)) {
                throw new InvalidPersonException("Person " + number + ": the contact is already in use");
            }
        }
        for (PersonRequest line : request.people()) {
            RollupFigures before = rollup.figures(sheetId);
            Person person = people.add(sheetId, line.name(), line.grade(),
                    line.terms().isEmpty() ? null : Integer.valueOf(line.terms()),
                    line.contact().isEmpty() ? null : line.contact());
            audit.recordOnCommit("PERSON_ADDED sheet=" + sheetId + " by=" + request.user()
                    + " name=" + person.getName() + " grade=" + person.getGrade()
                    + (person.getTerms() == null ? "" : " terms=" + person.getTerms())
                    + (person.getContact() == null ? "" : " contact=" + person.getContact()));
            rollup.recordChanges(sheetId, request.user(), before);
        }
        response.send(new ImportPeopleResponse(request.people().size()));
    }
}
