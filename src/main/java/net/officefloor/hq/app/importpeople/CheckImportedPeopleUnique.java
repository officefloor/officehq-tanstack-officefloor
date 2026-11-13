package net.officefloor.hq.app.importpeople;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.person.BuildPerson;
import net.officefloor.hq.app.person.CheckPersonNameUnique;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.NameTree;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Builds each imported person as the person form does and checks them with its rules against the sheet and its tree
 * (names, aliases, contacts). Any line refused refuses the whole import, before anything is written.
 */
public class CheckImportedPeopleUnique {
    public void check(@Val ImportedPeople imported, @Val Sheet sheet, PersonRepository repository, NameTree tree,
            Out<ImportedPersons> built) throws InvalidPersonException {
        List<Person> people = new ArrayList<>();
        int number = 0;
        for (PersonRequest request : imported.people()) {
            number++;
            Person[] person = new Person[1];
            new BuildPerson().build(request, sheet, value -> person[0] = value);
            try {
                new CheckPersonNameUnique().check(person[0], repository, tree);
            } catch (InvalidPersonException e) {
                throw new InvalidPersonException("Person " + number + ": " + e.getMessage());
            }
            people.add(person[0]);
        }
        built.set(new ImportedPersons(people));
    }
}
