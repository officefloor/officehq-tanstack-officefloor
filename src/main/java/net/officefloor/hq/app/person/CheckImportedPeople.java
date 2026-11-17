package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Matches each imported line to the person on the sheet with its name or alias (as names are compared), and checks
 *  every line before any is stored: a matched line as the person form checks an edit (see CheckPersonRename and
 *  CheckPersonRecontact and CheckPersonRegrade), any other as it checks an add (see CheckNewPersonName,
 *  CheckNewPersonGrade and CheckPersonContact). Two lines
 *  matching the same person, or any failing line, refuse the whole import. */
public class CheckImportedPeople {
    public void check(@PathVariable("sheetId") String sheetId, @Val ImportedPeople imported, PersonRepository people,
            GradeRepository grades, Out<ImportMatches> matches)
            throws DuplicatePersonNameException, DuplicatePersonContactException, InvalidPersonException {
        String sheet = sheetId.trim();
        List<Person> matched = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (PersonValues values : imported.people()) {
            Person person = people.findByName(sheet, values.name());
            if (person == null) {
                if (people.nameTaken(sheet, values.name(), 0) || people.treeTaken(sheet, values.name())) {
                    throw new DuplicatePersonNameException(values.name());
                }
                grades.checkChoosable(sheet, values.grade());
                if (values.contact() != null && people.contactTaken(sheet, values.contact(), 0)) {
                    throw new DuplicatePersonContactException(values.contact());
                }
            } else {
                if (!seen.add(person.id())) {
                    throw new InvalidPersonException("The person " + person.name() + " is imported more than once");
                }
                PersonValues edit = AddImportedPeople.edit(person, values);
                new CheckPersonRename().check(person, edit, people);
                new CheckPersonRecontact().check(person, edit, people);
                CheckPersonRegrade.refuse(sheet, person, edit, grades);
            }
            matched.add(person);
        }
        matches.set(new ImportMatches(Collections.unmodifiableList(matched)));
    }
}
