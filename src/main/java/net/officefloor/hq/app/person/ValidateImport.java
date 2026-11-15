package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Splits the import into its lines (blank lines skipped) and checks each with the person form's own
 *  rules; a name or contact repeated within the import, or an import with no lines, is refused. */
public class ValidateImport {
    public void validate(@RequestBody ImportRequest request, Out<ImportLines> lines)
            throws InvalidPersonException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<PersonRequest> checked = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        for (String line : text.split("\\r?\\n")) {
            if (line.isBlank()) continue;
            String[] f = line.split(",", 4);
            PersonRequest person = PersonForm.check(new PersonRequest(f[0], f.length > 1 ? f[1] : "", null,
                    f.length > 2 ? f[2] : "", null, f.length > 3 ? f[3] : ""));
            if (!names.add(person.name().toLowerCase(Locale.ROOT))) {
                throw new InvalidPersonException("The name is repeated in the import: " + person.name());
            }
            if (person.contact() != null && !contacts.add(person.contact())) {
                throw new InvalidPersonException("The contact is repeated in the import: " + person.contact());
            }
            checked.add(person);
        }
        if (checked.isEmpty()) {
            throw new InvalidPersonException("Import at least one line of name,grade,terms,contact");
        }
        lines.set(new ImportLines(checked));
    }
}
