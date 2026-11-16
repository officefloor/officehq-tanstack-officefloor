package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body and checks every line before any is kept: each non-blank line is name,period,amount — a person on the
 *  sheet by name or alias (case and outer spaces ignored), a period on the sheet by name (case ignored) and an amount of
 *  0.00 or more with two decimals. A failing line, or a paste with no lines, is refused. */
public class ValidateImportOpenings {
    private static final Pattern AMOUNT = Pattern.compile("\\d+\\.\\d{2}");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ImportOpeningsRequest request,
            PersonRepository people, PeriodRepository periods, Out<ImportedOpenings> imported)
            throws InvalidOpeningException {
        String sheet = sheetId.trim();
        String text = request == null || request.text() == null ? "" : request.text();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        List<Period> sheetPeriods = periods.list(sheet);
        List<OpeningLine> lines = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length != 3) {
                throw new InvalidOpeningException("A line is name,period,amount");
            }
            String name = fields[0].trim();
            Person person = name.isEmpty() ? null : people.findByName(sheet, name);
            if (person == null) {
                throw new InvalidOpeningException("No person " + name + " on the sheet");
            }
            String periodName = fields[1].trim();
            Period period = sheetPeriods.stream().filter((p) -> p.name().equalsIgnoreCase(periodName)).findFirst()
                    .orElse(null);
            if (period == null) {
                throw new InvalidOpeningException("No period " + periodName + " on the sheet");
            }
            String amount = fields[2].trim();
            if (!AMOUNT.matcher(amount).matches()) {
                throw new InvalidOpeningException("An amount is 0.00 or more with two decimals");
            }
            lines.add(new OpeningLine(person, period, new BigDecimal(amount)));
        }
        if (lines.isEmpty()) {
            throw new InvalidOpeningException("Paste at least one line of name,period,amount");
        }
        imported.set(new ImportedOpenings(lines, by));
    }
}
