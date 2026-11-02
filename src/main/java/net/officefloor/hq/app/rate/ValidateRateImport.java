package net.officefloor.hq.app.rate;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.grade.Grades;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Checks every imported line (grade,rate,from,to; the last two may be absent or blank) by the rate form's rules before
 * anything is written, so a refused import sets nothing. Blank lines are skipped; an import with no line is refused.
 */
public class ValidateRateImport {
    public void validate(@PathVariable("sheet") String sheet, @RequestBody RateImportRequest request, Grades grades,
            Out<ImportedRates> imported)
            throws InvalidRateException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<RateInput> lines = new ArrayList<>();
        List<String> sheetGrades = grades.all(sheet);
        int at = 0;
        for (String line : text.split("\\r?\\n")) {
            at++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length < 2 || fields.length > 4) {
                throw new InvalidRateException("Line " + at + ": give grade,rate,from,to");
            }
            try {
                lines.add(ValidateRate.check(new RateRequest(fields[0], fields[1],
                        fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : ""), sheetGrades));
            } catch (InvalidRateException e) {
                throw new InvalidRateException("Line " + at + ": " + e.getMessage());
            }
        }
        if (lines.isEmpty()) {
            throw new InvalidRateException("Paste at least one line of grade,rate,from,to");
        }
        imported.set(new ImportedRates(lines));
    }
}
