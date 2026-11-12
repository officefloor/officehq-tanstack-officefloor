package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a plan by a viewer, with an amount the claim form would refuse for a claim made from it (one that is
 *  not a number with up to two decimals, or a negative one), an interval that is not a whole number of at least
 *  1, a day of the month that is not a whole number from 1 to 31, both an interval and a day or neither, a bad
 *  date or an end before the start. A blank start is the sheet's today. */
public class ValidatePlan {
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");
    private static final Pattern EVERY = Pattern.compile("\\d{1,6}");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PlanRequest request,
            SheetStore sheets, Out<PlanInput> body, RoleStore roles) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheetId.trim(), user);
        if (request == null) {
            throw new InvalidSheetException("A plan needs an amount, a start, an interval and an end");
        }
        String amount = request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidSheetException("The amount must be a number with up to two decimals");
        }
        String every = request.every() == null ? "" : request.every().trim();
        String day = request.day() == null ? "" : request.day().trim();
        if (!every.isEmpty() && !day.isEmpty()) {
            throw new InvalidSheetException("A plan runs either every N days or on a day of the month, not both");
        }
        Integer everyDays = null;
        Integer monthDay = null;
        if (!day.isEmpty()) {
            if (!EVERY.matcher(day).matches() || Integer.parseInt(day) < 1 || Integer.parseInt(day) > 31) {
                throw new InvalidSheetException("The day must be a day of the month from 1 to 31");
            }
            monthDay = Integer.parseInt(day);
        } else {
            if (!EVERY.matcher(every).matches() || Integer.parseInt(every) < 1) {
                throw new InvalidSheetException("Every must be a whole number of days, at least 1");
            }
            everyDays = Integer.parseInt(every);
        }
        String startText = request.start() == null ? "" : request.start().trim();
        LocalDate start = startText.isEmpty() ? sheets.today(sheetId.trim()) : date(startText);
        LocalDate end = date(request.end() == null ? "" : request.end().trim());
        if (end.isBefore(start)) {
            throw new InvalidSheetException("The end must not be before the start");
        }
        body.set(new PlanInput(new BigDecimal(amount).setScale(2), start, everyDays, monthDay, end, user));
    }

    private static LocalDate date(String text) throws InvalidSheetException {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + text);
        }
    }
}
