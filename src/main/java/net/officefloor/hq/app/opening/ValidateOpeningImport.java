package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written. Each non-blank line is name,period,amount with
 *  an amount of two decimals, 0.00 or more. A paste with no lines is refused. A refusal names the first line that
 *  fails. */
public class ValidateOpeningImport {

    private static final Pattern AMOUNT = Pattern.compile("\\d+\\.\\d{2}");

    public void validate(@RequestBody OpeningImportRequest request, Out<OpeningImportInput> body)
            throws InvalidOpeningException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidOpeningException("Sign in before pasting opening balances.");
        }
        String[] rows = (request.text() == null ? "" : request.text()).split("\\r?\\n|\\r", -1);
        List<OpeningLine> lines = new ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            if (rows[i].isBlank()) {
                continue;
            }
            String[] fields = rows[i].split(",", -1);
            if (fields.length != 3 || fields[0].isBlank() || fields[1].isBlank()) {
                throw new InvalidOpeningException("Line " + (i + 1) + ": a line is name,period,amount.");
            }
            String amount = fields[2].trim();
            if (!AMOUNT.matcher(amount).matches()) {
                throw new InvalidOpeningException("Line " + (i + 1) + ": the amount must have two decimals, 0.00 or more.");
            }
            lines.add(new OpeningLine(i + 1, fields[0].trim(), fields[1].trim(), new BigDecimal(amount)));
        }
        if (lines.isEmpty()) {
            throw new InvalidOpeningException("Paste at least one line: name,period,amount.");
        }
        body.set(new OpeningImportInput(request.user().trim(), lines));
    }
}
