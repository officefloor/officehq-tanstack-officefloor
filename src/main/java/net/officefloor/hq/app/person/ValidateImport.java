package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.hq.app.alias.Aliases;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Checks every imported line (name,grade,terms,contact; the last two may be absent or blank) by the person form's rules
 * before anything is written, so a refused import adds no one: the name must not be a name or alias on the sheet or its
 * tree, the contact not one already in use, and neither may repeat within the import (ignoring case). Blank lines are
 * skipped; an import with no line is refused.
 */
public class ValidateImport {

    public void validate(@PathVariable("sheet") String sheet, @RequestBody ImportRequest request, People people,
            Aliases aliases, Out<ImportedPeople> imported) throws InvalidPersonException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<PersonRequest> lines = new ArrayList<>();
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
                throw new InvalidPersonException("Line " + at + ": give name,grade,terms,contact");
            }
            PersonRequest checked;
            try {
                checked = ValidatePerson.check(new PersonRequest(fields[0], fields[1], null, null,
                        fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : ""));
            } catch (InvalidPersonException e) {
                throw new InvalidPersonException("Line " + at + ": " + e.getMessage());
            }
            String name = checked.name();
            if (!names.add(name.toLowerCase(Locale.ROOT))) {
                throw new InvalidPersonException("Line " + at + ": the name " + name + " is repeated in the import");
            }
            if (people.nameTaken(sheet, name, null) || aliases.taken(sheet, name) || people.usedInTree(sheet, name)) {
                throw new InvalidPersonException("Line " + at + ": the name is already in use: " + name);
            }
            String contact = checked.contactOrNull();
            if (contact != null) {
                if (!contacts.add(contact)) {
                    throw new InvalidPersonException("Line " + at + ": the contact " + contact + " is repeated in the import");
                }
                if (people.contactTaken(sheet, contact, null)) {
                    throw new InvalidPersonException("Line " + at + ": the contact is already in use: " + contact);
                }
            }
            lines.add(checked);
        }
        if (lines.isEmpty()) {
            throw new InvalidPersonException("Paste at least one line of name,grade,terms,contact");
        }
        imported.set(new ImportedPeople(lines));
    }
}
