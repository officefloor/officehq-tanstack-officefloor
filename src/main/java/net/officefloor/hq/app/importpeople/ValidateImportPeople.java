package net.officefloor.hq.app.importpeople;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.person.PersonRules;
import net.officefloor.hq.app.person.TreeNames;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Reads the import box from the body: each non-blank line is name,grade,terms,contact — a blank grade is the sheet's default grade; the terms and the contact
 * may be blank or absent. Checks every line exactly as the person form would, refusing the whole import when any
 * line fails, a name (ignoring case) or a contact repeats one of an earlier line, or there is no line.
 */
public class ValidateImportPeople {
    public void validate(@RequestBody ImportPeopleRequest request, Out<ImportedLines> linesOut)
            throws InvalidPersonException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<PersonRequest> people = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        int at = 0;
        for (String line : text.split("\\r?\\n")) {
            at++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length < 2 || fields.length > 4) {
                throw new InvalidPersonException("Line " + at + ": a line is name,grade,terms,contact.");
            }
            PersonRequest person;
            try {
                boolean blankGrade = fields[1].trim().isEmpty();
                person = PersonRules.check(fields[0], blankGrade ? "A" : fields[1], null, null,
                        fields.length > 2 ? fields[2] : null, fields.length > 3 ? fields[3] : null, null);
                if (blankGrade) {
                    // Left blank: the sheet's default grade, known once the sheet is loaded.
                    person = person.withGrade("");
                }
            } catch (InvalidPersonException e) {
                throw new InvalidPersonException("Line " + at + ": " + e.getMessage());
            }
            if (!names.add(TreeNames.key(person.name()))) {
                throw new InvalidPersonException("Line " + at + ": the name " + person.name()
                        + " is already used in this import.");
            }
            if (person.contactOrNull() != null && !contacts.add(person.contactOrNull())) {
                throw new InvalidPersonException("Line " + at + ": the contact " + person.contactOrNull()
                        + " is already used in this import.");
            }
            people.add(person);
        }
        if (people.isEmpty()) {
            throw new InvalidPersonException("Paste at least one line of name,grade,terms,contact.");
        }
        linesOut.set(new ImportedLines(List.copyOf(people)));
    }
}
