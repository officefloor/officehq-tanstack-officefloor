package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Splits the import into lines (blank lines skipped) and checks each by the person form's rules; the
 * first line that fails refuses the whole import. An import with no lines is refused too. A line with
 * a blank grade takes the sheet's default grade.
 */
public class ValidateImportPeople {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ImportPeopleRequest request,
            SettingsRepository settings, GradeRepository grades, Out<ImportPeopleLines> linesOut)
            throws InvalidPersonException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<PersonRequest> lines = new ArrayList<>();
        int number = 0;
        String defaultGrade = null;
        for (String raw : text.split("\\r?\\n|\\r")) {
            number++;
            if (raw.isBlank()) continue;
            String[] fields = raw.split(",", 4);
            if (fields.length < 2) throw new InvalidPersonException("Line " + number + ": name,grade,terms,contact");
            String terms = fields.length > 2 ? fields[2] : "";
            String contact = fields.length > 3 ? fields[3] : "";
            String grade = fields[1];
            if (grade.isBlank()) {
                if (defaultGrade == null) defaultGrade = grades.newPersonGrade(settings.find(sheetId));
                grade = defaultGrade;
            }
            try {
                lines.add(PersonRequest.checked(new PersonRequest(fields[0], grade, null, "", terms, contact, null, "")));
            } catch (InvalidPersonException e) {
                throw new InvalidPersonException("Line " + number + ": " + e.getMessage());
            }
        }
        if (lines.isEmpty()) throw new InvalidPersonException("Paste at least one line of name,grade,terms,contact");
        linesOut.set(new ImportPeopleLines(lines));
    }
}
