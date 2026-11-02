package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Adds a plan for the person: an amount of 0.00 or more with up to two decimals, a start date (blank: the sheet's
 * today), every N days (a whole number, at least 1) and an end date not before the start.
 */
public class AddPlan {
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");
    private static final Pattern EVERY = Pattern.compile("\\d{1,9}");

    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @RequestBody PlanRequest request, Plans plans, SheetClock clock, Audit audit) throws InvalidPlanException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String start = request == null || request.start() == null ? "" : request.start().trim();
        String every = request == null || request.every() == null ? "" : request.every().trim();
        String end = request == null || request.end() == null ? "" : request.end().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidPlanException("The amount must be 0.00 or more with up to two decimals");
        }
        if (!EVERY.matcher(every).matches() || Integer.parseInt(every) < 1) {
            throw new InvalidPlanException("Every must be a whole number of days, at least 1");
        }
        LocalDate from;
        LocalDate to;
        try {
            from = start.isEmpty() ? clock.today(sheet) : LocalDate.parse(start);
            to = LocalDate.parse(end);
        } catch (DateTimeParseException e) {
            throw new InvalidPlanException("A plan needs a valid start and end date");
        }
        if (to.isBefore(from)) {
            throw new InvalidPlanException("A plan may not end before it starts");
        }
        BigDecimal value = new BigDecimal(amount).setScale(2);
        int days = Integer.parseInt(every);
        plans.add(sheet, person.id(), value, from, days, to);
        audit.record("PLAN_ADDED sheet=" + sheet + " by=" + user + " name=" + person.name() + " amount="
                + value.toPlainString() + " start=" + from + " every=" + days + " end=" + to);
    }
}
