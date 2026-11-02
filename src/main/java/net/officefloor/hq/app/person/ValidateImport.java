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
 * before anything is written, so a refused import changes no one. A line whose name is a name or alias of a person on
 * the sheet updates them (refused when they clash with the tree, as the form refuses); any other line's name must not be
 * in use in the tree. A contact must not be another person's, and neither a person nor a contact may repeat within the
 * import (ignoring case). Blank lines are
 * skipped; an import with no line is refused.
 */
public class ValidateImport {

    public void validate(@PathVariable("sheet") String sheet, @RequestBody ImportRequest request, People people,
            Aliases aliases, Out<ImportedPeople> imported) throws InvalidPersonException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<ImportedPeople.Line> lines = new ArrayList<>();
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
                        fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : "", null));
            } catch (InvalidPersonException e) {
                throw new InvalidPersonException("Line " + at + ": " + e.getMessage());
            }
            String name = checked.name();
            Person existing = people.findByName(sheet, name);
            // Two lines naming the same person (by name or alias) repeat them, as two lines of one new name do.
            String key = existing == null ? name.toLowerCase(Locale.ROOT) : "#" + existing.id();
            if (!names.add(key)) {
                throw new InvalidPersonException("Line " + at + ": the name " + name + " is repeated in the import");
            }
            if (existing != null) {
                // Updating them is an edit as the form makes one: a person clashing with the tree is refused.
                if (existing.clashing()) {
                    throw new InvalidPersonException("Line " + at + ": the name is already in use in the tree: " + name);
                }
            } else if (people.nameTaken(sheet, name, null) || aliases.taken(sheet, name)
                    || people.usedInTree(sheet, name)) {
                throw new InvalidPersonException("Line " + at + ": the name is already in use: " + name);
            }
            String contact = checked.contactOrNull();
            if (contact != null) {
                if (!contacts.add(contact)) {
                    throw new InvalidPersonException("Line " + at + ": the contact " + contact + " is repeated in the import");
                }
                if (people.contactTaken(sheet, contact, existing == null ? null : existing.id())) {
                    throw new InvalidPersonException("Line " + at + ": the contact is already in use: " + contact);
                }
            }
            lines.add(new ImportedPeople.Line(checked, existing));
        }
        if (lines.isEmpty()) {
            throw new InvalidPersonException("Paste at least one line of name,grade,terms,contact");
        }
        imported.set(new ImportedPeople(lines));
    }
}
