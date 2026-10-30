package net.officefloor.hq.app.rate;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRateImport {
    // Every line is checked before any is stored, by the rate form's rules: grade,rate,from,to (the last two may be
    // absent or blank). Blank lines are skipped; an import with no line is refused.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RateImportRequest body,
            net.officefloor.hq.app.grade.Grades grades, Out<ImportedRates> out) throws InvalidRateException {
        List<String> sheetGrades = grades.all(sheetId.trim());
        String text = body == null || body.text() == null ? "" : body.text();
        List<Rate> rates = new ArrayList<>();
        int n = 0;
        for (String line : text.split("\\r?\\n")) {
            n++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length < 2 || fields.length > 4) {
                throw new InvalidRateException("Line " + n + ": type grade,rate,from,to.");
            }
            try {
                rates.add(ValidateRate.parse(sheetId, fields[0], new RateRequest(fields[1],
                        fields.length > 2 ? fields[2] : "", fields.length > 3 ? fields[3] : ""), sheetGrades));
            } catch (InvalidRateException e) {
                throw new InvalidRateException("Line " + n + ": " + e.getMessage());
            }
        }
        if (rates.isEmpty()) {
            throw new InvalidRateException("Paste at least one line of grade,rate,from,to.");
        }
        out.set(new ImportedRates(rates));
    }
}
