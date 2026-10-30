package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.plan.InvalidPlanException;
import net.officefloor.hq.app.plan.PlanRequest;
import net.officefloor.hq.app.plan.ValidPlan;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** A plan's amount must be one the claim form takes for a new claim, its interval a whole number of days of at least 1
 *  or (instead) its day of the month a whole number from 1 to 31,
 *  its end a date (its start a date, or blank for the sheet's today). */
public class ValidatePlan {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PlanRequest request, Out<ValidPlan> body,
            Settings settings) throws InvalidPlanException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        java.math.BigDecimal amount;
        try {
            amount = ValidateClaim.amount(request.amount());
        } catch (InvalidClaimException e) {
            throw new InvalidPlanException(e.getMessage());
        }
        String every = request.every() == null ? "" : request.every().trim();
        String dayText = request.day() == null ? "" : request.day().trim();
        Integer days = null;
        Integer day = null;
        if (!dayText.isEmpty()) {
            if (!every.isEmpty()) {
                throw new InvalidPlanException("A plan runs either every N days or monthly on a day, not both");
            }
            if (!dayText.matches("\\d{1,2}") || (day = Integer.parseInt(dayText)) < 1 || day > 31) {
                throw new InvalidPlanException("A plan's day of the month must be a whole number from 1 to 31");
            }
        } else if (!every.matches("\\d{1,6}") || (days = Integer.parseInt(every)) < 1) {
            throw new InvalidPlanException("A plan's interval must be a whole number of days, at least 1");
        }
        LocalDate start = date(request.start(), true);
        LocalDate end = date(request.end(), false);
        if (start != null && end.isBefore(start)) {
            throw new InvalidPlanException("A plan's end cannot be before its start");
        }
        body.set(new ValidPlan(amount, start, days, day, end, request.user()));
    }

    private static LocalDate date(String raw, boolean blankAllowed) throws InvalidPlanException {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) {
            if (blankAllowed) return null;
            throw new InvalidPlanException("A plan needs an end date");
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidPlanException("The date is not a date");
        }
    }
}
