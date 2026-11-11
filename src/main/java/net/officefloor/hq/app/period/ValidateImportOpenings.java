package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Splits the import into lines (blank lines skipped) and checks each is name,period,amount with an
 * amount of two decimals, 0.00 or more; the first line that fails refuses the whole import. An import
 * with no lines is refused too.
 */
public class ValidateImportOpenings {
    public void validate(@RequestBody ImportOpeningsRequest request, Out<ImportOpeningsLines> linesOut)
            throws InvalidPeriodException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<OpeningLine> lines = new ArrayList<>();
        int number = 0;
        for (String raw : text.split("\\r?\\n|\\r")) {
            number++;
            if (raw.isBlank()) continue;
            String[] fields = raw.split(",", -1);
            if (fields.length != 3) throw new InvalidPeriodException("Line " + number + ": name,period,amount");
            String name = fields[0].trim();
            String period = fields[1].trim();
            String amount = fields[2].trim();
            if (name.isEmpty()) throw new InvalidPeriodException("Line " + number + ": a name is required");
            if (period.isEmpty()) throw new InvalidPeriodException("Line " + number + ": a period is required");
            if (!amount.matches("\\d+\\.\\d{2}")) {
                throw new InvalidPeriodException("Line " + number + ": an amount of two decimals, 0.00 or more");
            }
            lines.add(new OpeningLine(name, period, new BigDecimal(amount)));
        }
        if (lines.isEmpty()) throw new InvalidPeriodException("Paste at least one line of name,period,amount");
        linesOut.set(new ImportOpeningsLines(lines));
    }
}
