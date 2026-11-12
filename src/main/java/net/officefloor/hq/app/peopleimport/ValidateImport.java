package net.officefloor.hq.app.peopleimport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.person.PersonRules;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Checks every pasted line with exactly the person form's rules before anything is stored: each line
 * is name,grade,terms,contact (terms and contact may be absent or blank), the name and contact free on
 * the sheet (and the name on its tree) as the form requires, and no name (ignoring case and the spaces
 * around it) or contact given by an earlier line. Blank lines are skipped; a paste with no line is refused.
 */
public class ValidateImport {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ImportRequest request,
            PersonStore store, SettingStore settings, RoleStore roles, Out<ImportInput> imported)
            throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        String sheet = sheetId.trim();
        String text = request == null || request.text() == null ? "" : request.text();
        String[] lines = text.split("\\r?\\n");
        List<PersonRequest> people = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length < 2 || fields.length > 4) {
                throw new InvalidSheetException("Line " + (i + 1) + " must be name,grade,terms,contact");
            }
            try {
                PersonRequest person = PersonRules.check(new PersonRequest(fields[0], fields[1].trim(), null,
                        request.user(), null, fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : ""));
                PersonRules.refuseTaken(sheet, person, store, settings);
                if (!names.add(person.name().toLowerCase(Locale.ROOT))) {
                    throw new InvalidSheetException("The name " + person.name() + " is given twice");
                }
                if (person.contact() != null && !contacts.add(person.contact())) {
                    throw new InvalidSheetException("The contact " + person.contact() + " is given twice");
                }
                people.add(person);
            } catch (InvalidSheetException e) {
                throw new InvalidSheetException("Line " + (i + 1) + ": " + e.getMessage());
            }
        }
        if (people.isEmpty()) {
            throw new InvalidSheetException("Paste at least one line of name,grade,terms,contact");
        }
        imported.set(new ImportInput(people));
    }
}
