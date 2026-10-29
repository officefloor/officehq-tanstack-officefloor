package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateImport {
    // Every line is checked before any is added, by the person form's rules: name,grade,terms,contact (the last two may be
    // absent or blank). A name already used on the sheet's tree as a name or alias, or a contact already on the sheet, is
    // refused, as is a name or contact repeated in the import (ignoring case). Blank lines are skipped; an import with no
    // line is refused.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ImportRequest body, People people,
            Out<ImportedPeople> out) throws InvalidPersonException {
        String sheet = sheetId.trim();
        String text = body == null || body.text() == null ? "" : body.text();
        List<PersonRequest> lines = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        int n = 0;
        for (String line : text.split("\\r?\\n")) {
            n++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length < 2 || fields.length > 4) {
                throw new InvalidPersonException("Line " + n + ": type name,grade,terms,contact.");
            }
            try {
                PersonRequest request = PersonRules.parse(new PersonRequest(fields[0], fields[1], null, null,
                        fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : ""));
                if (!names.add(request.name().toLowerCase(Locale.ROOT))) {
                    throw new InvalidPersonException("The name " + request.name() + " is given twice in the import.");
                }
                if (request.contact() != null && !contacts.add(request.contact())) {
                    throw new InvalidPersonException("The contact " + request.contact() + " is given twice in the import.");
                }
                if (people.nameTaken(sheet, request.name(), -1)) {
                    throw new InvalidPersonException("The name is already in use.");
                }
                if (request.contact() != null && people.contactTaken(sheet, request.contact(), -1)) {
                    throw new InvalidPersonException("The contact is already in use.");
                }
                lines.add(request);
            } catch (InvalidPersonException e) {
                throw new InvalidPersonException("Line " + n + ": " + e.getMessage());
            }
        }
        if (lines.isEmpty()) {
            throw new InvalidPersonException("Paste at least one line of name,grade,terms,contact.");
        }
        out.set(new ImportedPeople(lines));
    }
}
