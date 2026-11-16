package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: each non-blank line is name,grade,terms,contact (terms and contact may be absent or blank), read by
 *  the person form's rules (see PersonValues#from). A name (case ignored) or contact repeated within the paste, or a
 *  paste with no lines, is refused. */
public class ValidateImportPeople {
    public void validate(@RequestBody ImportPeopleRequest request, Out<ImportedPeople> imported) throws InvalidPersonException {
        String text = request == null || request.text() == null ? "" : request.text();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        List<PersonValues> people = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        for (String line : text.split("\\r?\\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", 4);
            PersonValues values = PersonValues.from(new PersonRequest(fields[0], fields.length > 1 ? fields[1] : "", by, null,
                    "", fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : "", null));
            if (!names.add(PersonRepository.key(values.name()))) {
                throw new InvalidPersonException("The name " + values.name() + " is imported more than once");
            }
            if (values.contact() != null && !contacts.add(values.contact())) {
                throw new InvalidPersonException("The contact " + values.contact() + " is imported more than once");
            }
            people.add(values);
        }
        if (people.isEmpty()) {
            throw new InvalidPersonException("Paste at least one line of name,grade,terms,contact");
        }
        imported.set(new ImportedPeople(people, by));
    }
}
