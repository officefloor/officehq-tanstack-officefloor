package net.officefloor.hq.app.opening;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Splits the text into lines of name,period,amount (blank lines are skipped) and checks each line's shape: a name, a
 * period and an amount of two decimals, 0.00 or more. Any line refused, or nothing to import, refuses the whole import
 * before anything is written.
 */
public class ValidateImportOpeningsRequest {

    private static final Pattern AMOUNT = Pattern.compile("\\d{1,13}\\.\\d{2}");

    public void validate(@RequestBody @Valid ImportOpeningsRequest request, Out<PastedOpenings> pasted)
            throws InvalidOpeningException {
        String text = request.text() == null ? "" : request.text();
        List<PastedOpening> lines = new ArrayList<>();
        int number = 0;
        for (String line : text.split("\\r?\\n")) {
            number++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length != 3) {
                throw new InvalidOpeningException("Line " + number + ": a line is name,period,amount.");
            }
            String name = fields[0].trim();
            String period = fields[1].trim();
            String amount = fields[2].trim();
            if (name.isEmpty() || name.length() > 100) {
                throw new InvalidOpeningException("Line " + number + ": give the person's name.");
            }
            if (period.isEmpty() || period.length() > 100) {
                throw new InvalidOpeningException("Line " + number + ": give the period.");
            }
            if (!AMOUNT.matcher(amount).matches()) {
                throw new InvalidOpeningException("Line " + number + ": the amount must have two decimals, 0.00 or more.");
            }
            lines.add(new PastedOpening(name, period, new BigDecimal(amount), null, null));
        }
        if (lines.isEmpty()) {
            throw new InvalidOpeningException("Paste at least one line of name,period,amount.");
        }
        pasted.set(new PastedOpenings(lines));
    }
}
