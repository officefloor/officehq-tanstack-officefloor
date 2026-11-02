package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Changes the plan's amount from the sheet's today: 0.00 or more with up to two decimals, as a plan is added with. The
 * claims generated from then on take the new amount; those already generated keep theirs.
 */
public class ChangePlanAmount {
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void change(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Plan plan,
            @RequestBody PlanAmountRequest request, Plans plans, People people, SheetClock clock, Audit audit)
            throws InvalidPlanException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidPlanException("The amount must be 0.00 or more with up to two decimals");
        }
        BigDecimal value = new BigDecimal(amount).setScale(2);
        plans.changeAmount(plan.id(), clock.today(sheet), value);
        Person person = people.find(sheet, plan.person());
        audit.record("PLAN_CHANGED sheet=" + sheet + " by=" + user + " name=" + (person == null ? "" : person.name())
                + " start=" + plan.start() + " amount=" + value.toPlainString());
    }
}
