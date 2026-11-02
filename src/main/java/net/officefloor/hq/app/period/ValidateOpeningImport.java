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

/**
 * Checks every pasted opening-balance line (name,period,amount: a person on the sheet by name or alias, a period on
 * the sheet, an amount of two decimals, 0.00 or more) before anything is written, so a refused paste sets nothing.
 * Blank lines are skipped; a paste with no line is refused.
 */
public class ValidateOpeningImport {

    private static final Pattern AMOUNT = Pattern.compile("\\d+\\.\\d{2}");

    public void validate(@PathVariable("sheet") String sheet, @RequestBody OpeningImportRequest request,
            People people, Periods periods, Out<ImportedOpenings> imported) throws InvalidPeriodException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<ImportedOpenings.Line> lines = new ArrayList<>();
        int at = 0;
        for (String line : text.split("\\r?\\n")) {
            at++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length != 3) {
                throw new InvalidPeriodException("Line " + at + ": give name,period,amount");
            }
            String name = fields[0].trim();
            Person person = name.isEmpty() ? null : people.findByName(sheet, name);
            if (person == null) {
                throw new InvalidPeriodException("Line " + at + ": no one on the sheet is named " + name);
            }
            String period = fields[1].trim();
            if (period.isEmpty() || !periods.named(sheet, period)) {
                throw new InvalidPeriodException("Line " + at + ": no period on the sheet is named " + period);
            }
            String amount = fields[2].trim();
            if (!AMOUNT.matcher(amount).matches()) {
                throw new InvalidPeriodException("Line " + at + ": the amount must be two decimals, 0.00 or more");
            }
            lines.add(new ImportedOpenings.Line(person.id(), person.name(), period, new BigDecimal(amount)));
        }
        if (lines.isEmpty()) {
            throw new InvalidPeriodException("Paste at least one line of name,period,amount");
        }
        imported.set(new ImportedOpenings(lines));
    }
}
