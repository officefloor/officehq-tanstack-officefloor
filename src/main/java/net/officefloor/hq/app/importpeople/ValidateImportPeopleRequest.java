package net.officefloor.hq.app.importpeople;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.person.ValidatePersonRequest;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Splits the text into lines of name,grade,terms,contact (terms and contact may be absent or blank; blank lines are
 * skipped) and checks each with exactly the person form's rules. Nothing pasted is refused, as is a line with more
 * than four fields, and a name (ignoring case) or contact an earlier line gave. A blank grade is kept blank, for the
 * sheet's default grade.
 */
public class ValidateImportPeopleRequest {
    public void validate(@RequestBody @Valid ImportPeopleRequest request, Out<ImportedPeople> imported)
            throws InvalidPersonException {
        String text = request.text() == null ? "" : request.text();
        List<PersonRequest> people = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        int number = 0;
        for (String line : text.split("\\r?\\n")) {
            number++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length > 4) {
                throw new InvalidPersonException("Line " + number + ": too many fields.");
            }
            String name = fields[0];
            String grade = fields.length > 1 ? fields[1] : "";
            String terms = fields.length > 2 ? fields[2] : "";
            String contact = fields.length > 3 ? fields[3] : "";
            // The person form's field limits, which its request checks on binding.
            if (name.length() > 200 || grade.length() > 10 || terms.length() > 30 || contact.length() > 200) {
                throw new InvalidPersonException("Line " + number + ": a field is too long.");
            }
            PersonRequest[] person = new PersonRequest[1];
            try {
                new ValidatePersonRequest().validate(new PersonRequest(name, grade.isBlank() ? "A" : grade, "", null,
                        terms, contact, null), value -> person[0] = value);
            } catch (InvalidPersonException e) {
                throw new InvalidPersonException("Line " + number + ": " + e.getMessage());
            }
            if (!names.add(person[0].name().toLowerCase(Locale.ROOT))) {
                throw new InvalidPersonException("Line " + number + ": the name " + person[0].name()
                        + " is already given by an earlier line.");
            }
            if (person[0].contactOrNull() != null && !contacts.add(person[0].contact())) {
                throw new InvalidPersonException("Line " + number + ": the contact " + person[0].contact()
                        + " is already given by an earlier line.");
            }
            if (grade.isBlank()) {
                PersonRequest p = person[0];
                person[0] = new PersonRequest(p.name(), "", p.gradeFrom(), p.version(), p.terms(), p.contact(),
                        p.tags(), p.termsFrom());
            }
            people.add(person[0]);
        }
        if (people.isEmpty()) {
            throw new InvalidPersonException("Paste at least one line of name,grade,terms,contact.");
        }
        imported.set(new ImportedPeople(people));
    }
}
