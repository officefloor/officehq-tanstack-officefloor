package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Changes a plan's amount on the sheet's today, recorded as PLAN_CHANGED sheet=<id> by=<user> name=<person>
 *  start=<plan start> amount=<new amount>. The claims it generates from then on are of the new amount; those
 *  already generated keep theirs. Publishes the plan as it now is. */
public class ChangePlanAmount {
    public void change(@Val PlanAmountRequest request, @Val Plan plan, PlanStore store, SheetStore sheets,
            PersonStore people, Audit audit, Out<Plan> changed) {
        BigDecimal amount = new BigDecimal(request.amount());
        LocalDate today = sheets.today(plan.sheetId());
        store.changeAmount(plan.id(), today, amount);
        Person person = people.find(plan.sheetId(), plan.personId());
        audit.record("PLAN_CHANGED sheet=" + plan.sheetId() + " by=" + request.user() + " name="
                + (person == null ? "" : person.getName()) + " start=" + plan.start() + " amount=" + request.amount());
        changed.set(store.find(plan.id()));
    }
}
