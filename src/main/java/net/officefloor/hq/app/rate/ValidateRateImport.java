package net.officefloor.hq.app.rate;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.grade.GradeStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Checks every imported line with exactly the rate form's rules before anything is stored. Each line is
 * grade,rate,from,to; the from and to may be absent or blank. Blank lines are skipped; an import with no line
 * is refused.
 */
public class ValidateRateImport {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RateImportRequest request,
            Out<RateImportInput> imported, RoleStore roles, GradeStore grades) throws InvalidSheetException {
        String user = request == null ? null : request.user();
        roles.refuseChange(sheetId.trim(), user);
        String text = request == null || request.text() == null ? "" : request.text();
        String[] lines = text.split("\\r?\\n");
        List<RateInput> rates = new ArrayList<>();
        List<String> sheetGrades = grades.grades(sheetId.trim());
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length < 2 || fields.length > 4) {
                throw new InvalidSheetException("Line " + (i + 1) + " must be grade,rate,from,to");
            }
            try {
                rates.add(ValidateRate.check(new RateRequest(fields[0].trim(), fields[1],
                        fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : "", user), sheetGrades));
            } catch (InvalidSheetException e) {
                throw new InvalidSheetException("Line " + (i + 1) + ": " + e.getMessage());
            }
        }
        if (rates.isEmpty()) {
            throw new InvalidSheetException("There are no rates to import");
        }
        imported.set(new RateImportInput(rates));
    }
}
