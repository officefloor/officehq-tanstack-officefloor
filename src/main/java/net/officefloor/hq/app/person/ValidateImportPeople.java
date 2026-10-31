package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Splits the import into lines of name,grade,terms,contact (terms and contact may be absent or blank)
 * and checks each by the person form's rules; a blank grade is the sheet's default grade. Blank lines are skipped; a name or contact given twice
 * in the import, or an import with no one in it, is refused.
 */
public class ValidateImportPeople {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ImportPeopleRequest request, Out<ValidImportPeople> body, Settings settings,
            net.officefloor.hq.app.grade.RetiredGrades grades)
            throws InvalidPersonException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String text = request.text() == null ? "" : request.text();
        List<PersonRequest> people = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        String defaultGrade = settings.defaultGrade(sheetId);
        if (grades.isRetired(sheetId, defaultGrade)) {
            defaultGrade = net.officefloor.hq.app.setting.Settings.START_DEFAULT_GRADE; // a retired default falls back to A
        }
        int number = 0;
        for (String raw : text.split("\\r?\\n")) {
            number++;
            if (raw.isBlank()) continue;
            String[] fields = raw.split(",", -1);
            if (fields.length < 2 || fields.length > 4) {
                throw new InvalidPersonException("Line " + number + ": a line is name,grade,terms,contact");
            }
            PersonRequest person;
            try {
                person = PersonRules.check(new PersonRequest(fields[0], fields[1].isBlank() ? defaultGrade : fields[1], request.user(), null, "",
                        fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : "", "", ""));
            } catch (InvalidPersonException e) {
                throw new InvalidPersonException("Line " + number + ": " + e.getMessage());
            }
            if (!names.add(person.name().toLowerCase(Locale.ROOT))) {
                throw new InvalidPersonException("Line " + number + ": the name " + person.name() + " is given twice in the import");
            }
            if (!person.contact().isEmpty() && !contacts.add(person.contact())) {
                throw new InvalidPersonException("Line " + number + ": the contact " + person.contact() + " is given twice in the import");
            }
            people.add(person);
        }
        if (people.isEmpty()) {
            throw new InvalidPersonException("Nothing to import: give one person per line");
        }
        body.set(new ValidImportPeople(people, request.user()));
    }
}
