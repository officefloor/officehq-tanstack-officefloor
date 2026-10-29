package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateOpeningImport {

    private static final Pattern AMOUNT = Pattern.compile("\\d{1,13}\\.\\d{2}");

    // Every line is checked before any is stored: name,period,amount — the name a person's name or alias on the sheet
    // (ignoring case and surrounding spaces), the period one on the sheet (open or closed), the amount with two decimals,
    // 0.00 or more. Blank lines are skipped; a paste with no line is refused.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody OpeningImportRequest body, People people,
            Periods periods, Out<ImportedOpenings> out) throws InvalidPeriodException {
        String sheet = sheetId.trim();
        String text = body == null || body.text() == null ? "" : body.text();
        List<ImportedOpenings.Line> lines = new ArrayList<>();
        int n = 0;
        for (String line : text.split("\\r?\\n")) {
            n++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length != 3) {
                throw new InvalidPeriodException("Line " + n + ": type name,period,amount.");
            }
            String name = fields[0].trim();
            Person person = name.isEmpty() ? null : people.findByName(sheet, name);
            if (person == null) {
                throw new InvalidPeriodException("Line " + n + ": no person " + name + " on this sheet.");
            }
            String periodName = fields[1].trim();
            Period period = periodName.isEmpty() ? null : periods.find(sheet, periodName);
            if (period == null) {
                throw new InvalidPeriodException("Line " + n + ": no period " + periodName + " on this sheet.");
            }
            String amount = fields[2].trim();
            if (!AMOUNT.matcher(amount).matches()) {
                throw new InvalidPeriodException("Line " + n + ": the amount must be 0.00 or more, with two decimals.");
            }
            lines.add(new ImportedOpenings.Line(person, period, new BigDecimal(amount)));
        }
        if (lines.isEmpty()) {
            throw new InvalidPeriodException("Paste at least one line of name,period,amount.");
        }
        out.set(new ImportedOpenings(lines));
    }
}
