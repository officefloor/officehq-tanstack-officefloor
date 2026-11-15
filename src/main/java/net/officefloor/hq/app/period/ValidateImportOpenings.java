package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Splits the paste into its lines (blank lines skipped) and checks each: a person on the sheet by name
 *  or alias, a period on the sheet, an amount of 0.00 or more with two decimals. A paste with no lines,
 *  or any line refused, is refused. */
public class ValidateImportOpenings {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ImportOpeningsRequest request,
            PersonRepository people, PeriodRepository periods, Out<OpeningLines> lines)
            throws InvalidPeriodException {
        String id = sheetId.trim();
        String text = request == null || request.text() == null ? "" : request.text();
        List<OpeningLines.Line> checked = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            if (line.isBlank()) continue;
            String[] f = line.split(",", -1);
            if (f.length != 3) {
                throw new InvalidPeriodException("Each line is name,period,amount: " + line);
            }
            Person person = people.findByName(id, f[0].trim());
            if (f[0].isBlank() || person == null) {
                throw new InvalidPeriodException("Not on the sheet: " + f[0].trim());
            }
            Period period = f[1].isBlank() ? null : periods.findByName(id, f[1].trim());
            if (period == null) {
                throw new InvalidPeriodException("No such period: " + f[1].trim());
            }
            String amount = f[2].trim();
            if (!amount.matches("\\d+\\.\\d{2}")) {
                throw new InvalidPeriodException("The amount is not 0.00 or more with two decimals: " + amount);
            }
            checked.add(new OpeningLines.Line(person.getId(), person.getName(), period.name(), new BigDecimal(amount)));
        }
        if (checked.isEmpty()) {
            throw new InvalidPeriodException("Paste at least one line of name,period,amount");
        }
        lines.set(new OpeningLines(checked));
    }
}
