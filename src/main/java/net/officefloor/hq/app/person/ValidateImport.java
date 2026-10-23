package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written. Each non-blank line is name,grade,terms,contact
 *  — the terms and contact may be absent or blank — and must pass exactly the person form's rules; no two lines may
 *  give the same name, or the same contact (compared as the sheet compares them). A paste with no lines is refused.
 *  A refusal names the first line that fails. */
public class ValidateImport {
    public void validate(@RequestBody ImportRequest request, Out<ImportInput> body) throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before changing people.");
        }
        String user = request.user().trim();
        String[] rows = (request.text() == null ? "" : request.text()).split("\\r?\\n|\\r", -1);
        List<ImportLine> lines = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        for (int i = 0; i < rows.length; i++) {
            if (rows[i].isBlank()) {
                continue;
            }
            int number = i + 1;
            String[] fields = rows[i].split(",", 4);
            try {
                PersonRequest person = ValidatePerson.check(new PersonRequest(user, fields[0],
                        fields.length > 1 ? fields[1] : "", null, null, fields.length > 2 ? fields[2] : "",
                        fields.length > 3 ? fields[3] : "", null, null));
                if (!names.add(person.name().toLowerCase(Locale.ROOT))) {
                    throw new DuplicatePersonNameException(person.name());
                }
                if (!person.contact().isEmpty() && !contacts.add(person.contact())) {
                    throw new DuplicatePersonContactException(person.contact());
                }
                lines.add(new ImportLine(number, person));
            } catch (InvalidPersonException | DuplicatePersonNameException | DuplicatePersonContactException e) {
                throw new InvalidPersonException("Line " + number + ": " + e.getMessage());
            }
        }
        if (lines.isEmpty()) {
            throw new InvalidPersonException("Paste at least one line: name,grade,terms,contact.");
        }
        body.set(new ImportInput(user, lines));
    }
}
