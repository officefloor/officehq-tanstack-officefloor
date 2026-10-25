package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Reads the opening balances paste box from the body: each non-blank line is name,period,amount, each trimmed; the
 * amount is 0.00 or more with up to two decimals. Refuses the whole paste when any line fails or there is none.
 */
public class ValidateOpenings {

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestBody OpeningsRequest request, Out<OpeningLines> linesOut) throws InvalidOpeningException {
        String text = request == null || request.text() == null ? "" : request.text();
        List<OpeningLine> lines = new ArrayList<>();
        int at = 0;
        for (String line : text.split("\\r?\\n")) {
            at++;
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            if (fields.length != 3 || fields[0].isBlank() || fields[1].isBlank()) {
                throw new InvalidOpeningException("Line " + at + ": a line is name,period,amount.");
            }
            String amount = fields[2].trim();
            if (!AMOUNT.matcher(amount).matches()) {
                throw new InvalidOpeningException("Line " + at + ": the amount must be 0.00 or more, with up to two decimals.");
            }
            lines.add(new OpeningLine(fields[0].trim(), fields[1].trim(), new BigDecimal(amount).setScale(2)));
        }
        if (lines.isEmpty()) {
            throw new InvalidOpeningException("Paste at least one line of name,period,amount.");
        }
        linesOut.set(new OpeningLines(List.copyOf(lines)));
    }
}
