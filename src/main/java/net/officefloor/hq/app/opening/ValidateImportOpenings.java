package net.officefloor.hq.app.opening;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * The opening balance paste's lines of name,period,amount (blank lines are skipped): the amount has two decimals and is
 * 0.00 or more. A refusal names the line. Publishes them parsed, in order, with the user making the change.
 */
public class ValidateImportOpenings {

    static final int MAX_LINES = 1000;

    private static final Pattern AMOUNT = Pattern.compile("\\d{1,12}\\.\\d{2}");

    public void validate(@RequestBody ImportOpeningsRequest request, Out<TypedOpenings> typed,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String text = request == null || request.lines() == null ? "" : request.lines();
        List<OpeningLine> lines = new ArrayList<>();
        String[] pasted = text.split("\\r?\\n");
        for (int i = 0; i < pasted.length; i++) {
            if (pasted[i].trim().isEmpty()) {
                continue;
            }
            String[] fields = pasted[i].split(",", -1);
            if (fields.length != 3) {
                throw new BadRequestException("Line " + (i + 1) + ": enter name,period,amount");
            }
            String name = fields[0].trim();
            String period = fields[1].trim();
            String amount = fields[2].trim();
            if (name.isEmpty()) {
                throw new BadRequestException("Line " + (i + 1) + ": enter a name");
            }
            if (period.isEmpty()) {
                throw new BadRequestException("Line " + (i + 1) + ": enter a period");
            }
            if (!AMOUNT.matcher(amount).matches()) {
                throw new BadRequestException("Line " + (i + 1) + ": enter an amount of 0.00 or more, with two decimals");
            }
            lines.add(new OpeningLine(i + 1, name, period, new BigDecimal(amount)));
        }
        if (lines.isEmpty()) {
            throw new BadRequestException("Paste at least one line of name,period,amount");
        }
        if (lines.size() > MAX_LINES) {
            throw new BadRequestException("Paste at most " + MAX_LINES + " lines at once");
        }
        typed.set(new TypedOpenings(lines));
        changedBy.set(by);
    }
}
