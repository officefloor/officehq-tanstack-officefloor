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
    // Every line is checked before any is stored, by the person form's rules: name,grade,terms,contact (the last two may be
    // absent or blank; a blank grade is the sheet's default grade, or A when that is retired). A retired grade is refused, unless the line matches a person and leaves them on it (the grade in force today). A line whose name matches a person on the sheet (by name or alias, as names are compared) updates
    // them, checked as their edit form would be; any other line adds, and is refused when its name is used on the sheet's
    // tree as a name or alias. A contact already used by someone else on the sheet is refused, as is a name, contact or
    // matched person repeated in the import (ignoring case). Blank lines are skipped; an import with no line is refused.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ImportRequest body, People people,
            net.officefloor.hq.app.setting.Settings settings, net.officefloor.hq.app.grade.Grades grades,
            net.officefloor.hq.app.sheet.Sheets sheets, Out<ImportedPeople> out) throws InvalidPersonException {
        String sheet = sheetId.trim();
        String text = body == null || body.text() == null ? "" : body.text();
        List<ImportedPeople.Line> lines = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<String> contacts = new HashSet<>();
        Set<Long> matched = new HashSet<>();
        String defaultGrade = grades.defaultFor(sheet, settings.defaultGrade(sheet));
        java.time.LocalDate today = sheets.today(sheet);
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
                PersonRequest request = PersonRules.parse(new PersonRequest(fields[0], fields[1].isBlank() ? defaultGrade : fields[1], null, null,
                        fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : "", null, null));
                if (!names.add(request.name().toLowerCase(Locale.ROOT))) {
                    throw new InvalidPersonException("The name " + request.name() + " is given twice in the import.");
                }
                if (request.contact() != null && !contacts.add(request.contact())) {
                    throw new InvalidPersonException("The contact " + request.contact() + " is given twice in the import.");
                }
                Person existing = people.findByName(sheet, request.name());
                if (existing != null) {
                    if (!matched.add(existing.id())) {
                        throw new InvalidPersonException(existing.name() + " is given twice in the import.");
                    }
                    if (people.aliasClashes(existing)) {
                        throw new InvalidPersonException("An alias of theirs is already in use on the sheet's tree.");
                    }
                } else if (people.nameTaken(sheet, request.name(), -1)) {
                    throw new InvalidPersonException("The name is already in use.");
                }
                if (grades.retired(sheet, request.grade())
                        && (existing == null || !request.grade().equals(existing.gradeOn(today)))) {
                    throw new InvalidPersonException("Grade " + request.grade() + " is retired.");
                }
                if (request.contact() != null
                        && people.contactTaken(sheet, request.contact(), existing == null ? -1 : existing.id())) {
                    throw new InvalidPersonException("The contact is already in use.");
                }
                lines.add(new ImportedPeople.Line(request, existing));
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
