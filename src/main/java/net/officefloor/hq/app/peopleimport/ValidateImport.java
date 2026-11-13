package net.officefloor.hq.app.peopleimport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.officefloor.hq.app.grade.GradeStore;
import net.officefloor.hq.app.person.DuplicateContactException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.person.PersonRules;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Checks every pasted line with exactly the person form's rules before anything is stored: each line
 * is name,grade,terms,contact (terms and contact may be absent or blank; a blank grade is the sheet's default grade, A while that is retired). A line whose name is a person's
 * name or alias on the sheet (as names are compared) updates that person, its contact free of anyone else on
 * the sheet; any other line adds, its name and contact free on the sheet (and the name on its tree) as the form
 * requires. No name (ignoring case and the spaces around it), person or contact may be given by an earlier
 * line. A retired grade cannot be chosen (see {@link GradeStore#refuseChoice}). Blank lines are skipped; a paste with no line is refused.
 */
public class ValidateImport {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ImportRequest request,
            PersonStore store, SettingStore settings, RoleStore roles, GradeStore grades, SheetStore sheets,
            Out<ImportInput> imported)
            throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        String sheet = sheetId.trim();
        String text = request == null || request.text() == null ? "" : request.text();
        String[] lines = text.split("\\r?\\n");
        List<ImportInput.Line> people = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<Long> updated = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        List<String> sheetGrades = grades.grades(sheet);
        String defaultGrade = settings.defaultGrade(sheet);
        if (grades.isRetired(sheet, defaultGrade)) {
            defaultGrade = "A";
        }
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
                PersonRequest person = PersonRules.check(new PersonRequest(fields[0],
                        fields[1].trim().isEmpty() ? defaultGrade : fields[1].trim(), null,
                        request.user(), null, fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : "", null, null),
                        sheetGrades);
                Person existing = store.findByName(sheet, person.name());
                grades.refuseChoice(sheet, person.grade(), existing, null, sheets.today(sheet));
                if (existing == null) {
                    PersonRules.refuseTaken(sheet, person, store, settings);
                } else if (person.contact() != null
                        && store.contactTaken(sheet, person.contact(), existing.getId())) {
                    throw new DuplicateContactException(person.contact());
                }
                if (!names.add(person.name().toLowerCase(Locale.ROOT))
                        || (existing != null && !updated.add(existing.getId()))) {
                    throw new InvalidSheetException("The name " + person.name() + " is given twice");
                }
                if (person.contact() != null && !contacts.add(person.contact())) {
                    throw new InvalidSheetException("The contact " + person.contact() + " is given twice");
                }
                people.add(new ImportInput.Line(person, existing));
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
