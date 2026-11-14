package net.officefloor.hq.app.importpeople;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.person.ApplyPerson;
import net.officefloor.hq.app.person.BuildPerson;
import net.officefloor.hq.app.person.CheckPersonNameUnique;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.NameTree;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.person.PersonTermsChange;
import net.officefloor.hq.app.person.StalePersonException;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Builds each imported person and checks them with the person form's rules against the sheet and its tree (names,
 * aliases, contacts). A line whose name is a person's name or alias on the sheet updates that person as the form's
 * edit does (grade from the sheet's today; terms and contact only when the line gives them); any other line adds a
 * person as the form's add does. Two lines for the same person, or any line refused, refuse the whole import, before
 * anything is written. A line with a blank grade takes the sheet's default grade (blank: A).
 */
public class CheckImportedPeopleUnique {
    public void check(@Val ImportedPeople imported, @Val Sheet sheet, PersonRepository repository, NameTree tree,
            SheetRepository sheets, SettingRepository settings, Out<ImportedPersons> built) throws InvalidPersonException {
        List<ImportedPersons.Entry> people = new ArrayList<>();
        Set<Long> updating = new HashSet<>();
        int number = 0;
        String defaultGrade = settings.defaultGradeOrA(sheet.id());
        for (PersonRequest line : imported.people()) {
            number++;
            PersonRequest request = line.grade().isEmpty()
                    ? new PersonRequest(line.name(), defaultGrade, line.gradeFrom(), line.version(), line.terms(),
                            line.contact(), line.tags(), line.termsFrom())
                    : line;
            try {
                Person existing = repository.findByName(sheet.id(), request.name());
                if (existing == null) {
                    Person[] person = new Person[1];
                    new BuildPerson().build(request, sheet, value -> person[0] = value);
                    new CheckPersonNameUnique().check(person[0], repository, tree);
                    people.add(new ImportedPersons.Entry(person[0], null));
                    continue;
                }
                if (!updating.add(existing.id())) {
                    throw new InvalidPersonException("The person " + existing.name()
                            + " is already given by an earlier line.");
                }
                PersonRequest edit = new PersonRequest(existing.name(), request.grade(), "", null,
                        request.termsDays() != null ? request.terms()
                                : existing.terms() == null ? "" : String.valueOf(existing.terms()),
                        request.contactOrNull() != null ? request.contact()
                                : existing.contact() == null ? "" : existing.contact(), null);
                Person[] person = new Person[1];
                PersonTermsChange[] change = new PersonTermsChange[1];
                try {
                    new ApplyPerson().apply(edit, existing, sheets, value -> person[0] = value,
                            value -> change[0] = value);
                } catch (StalePersonException e) {
                    throw new InvalidPersonException(e.getMessage());
                }
                new CheckPersonNameUnique().check(person[0], repository, tree);
                people.add(new ImportedPersons.Entry(person[0], change[0]));
            } catch (InvalidPersonException e) {
                throw new InvalidPersonException("Person " + number + ": " + e.getMessage());
            }
        }
        built.set(new ImportedPersons(people));
    }
}
