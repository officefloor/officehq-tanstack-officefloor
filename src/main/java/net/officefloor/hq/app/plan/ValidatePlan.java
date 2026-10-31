package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a plan whose amount is not more than 0 with up to two decimals, whose every is not a
 *  whole number of at least 1, or whose dates are not dates or end before the start. */
public class ValidatePlan {
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");
    private static final Pattern EVERY = Pattern.compile("\\d{1,6}");

    public void validate(@RequestBody PlanRequest request, Out<PlanValues> body) throws InvalidSheetRequestException {
        if (request == null) throw new InvalidSheetRequestException("A plan needs an amount, dates and every");
        String amount = request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches() || new BigDecimal(amount).signum() <= 0) {
            throw new InvalidSheetRequestException("The amount must be more than 0 with up to two decimals");
        }
        String every = request.every() == null ? "" : request.every().trim();
        if (!EVERY.matcher(every).matches() || Integer.parseInt(every) < 1) {
            throw new InvalidSheetRequestException("Every must be a whole number of days, at least 1");
        }
        LocalDate start = date(request.start(), "start");
        LocalDate end = date(request.end(), "end");
        if (end == null) throw new InvalidSheetRequestException("A plan needs an end date");
        if (start != null && end.isBefore(start)) {
            throw new InvalidSheetRequestException("The end date cannot be before the start");
        }
        body.set(new PlanValues(new BigDecimal(amount).setScale(2), start, Integer.parseInt(every), end, request.user()));
    }

    private static LocalDate date(String raw, String what) throws InvalidSheetRequestException {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) return null;
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetRequestException("The " + what + " is not a date (yyyy-mm-dd): " + text);
        }
    }
}
