package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.claim.ClaimForm;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * The person's new plan; refused (400) for an amount the claim form would refuse for a claim that corrects none, an
 * interval that is not a whole number of at least 1, a day of the month that is not a whole number from 1 to 31 (or
 * given with an interval too), a bad date, or an end before the start. A blank start is the
 * sheet's today.
 */
public class BuildPlan {

    private static final Pattern EVERY = Pattern.compile("\\d{1,6}");
    private static final Pattern DAY = Pattern.compile("\\d{1,2}");

    public void build(@Val Person person, @Val PlanRequest request, SheetRepository sheets, Out<Plan> plan)
            throws InvalidPlanException {
        BigDecimal amount;
        try {
            amount = ClaimForm.check(new ClaimRequest(request.amount(), "", "", "", "", "")).amount();
        } catch (InvalidClaimException e) {
            throw new InvalidPlanException(e.getMessage());
        }
        String every = request.every() == null ? "" : request.every().trim();
        String day = request.day() == null ? "" : request.day().trim();
        Integer everyDays = null;
        Integer monthDay = null;
        if (!day.isEmpty()) {
            if (!every.isEmpty()) {
                throw new InvalidPlanException("Give either every N days or a day of the month, not both.");
            }
            if (!DAY.matcher(day).matches() || Integer.parseInt(day) < 1 || Integer.parseInt(day) > 31) {
                throw new InvalidPlanException("The day of the month must be a whole number from 1 to 31.");
            }
            monthDay = Integer.parseInt(day);
        } else {
            if (!EVERY.matcher(every).matches() || Integer.parseInt(every) < 1) {
                throw new InvalidPlanException("Every must be a whole number of days, at least 1.");
            }
            everyDays = Integer.parseInt(every);
        }
        String startText = request.start() == null ? "" : request.start().trim();
        LocalDate start = startText.isEmpty() ? sheets.today(person.sheetId()) : date(startText, "start");
        String endText = request.end() == null ? "" : request.end().trim();
        if (endText.isEmpty()) {
            throw new InvalidPlanException("An end date is required.");
        }
        LocalDate end = date(endText, "end");
        if (end.isBefore(start)) {
            throw new InvalidPlanException("The end cannot be before the start.");
        }
        plan.set(new Plan(null, person.sheetId(), person.id(), amount, start, everyDays, monthDay, end,
                0, java.util.List.of()));
    }

    private static LocalDate date(String text, String what) throws InvalidPlanException {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidPlanException("The " + what + " date must be yyyy-mm-dd.");
        }
    }
}
