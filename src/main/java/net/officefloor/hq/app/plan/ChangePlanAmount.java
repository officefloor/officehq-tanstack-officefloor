package net.officefloor.hq.app.plan;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Changes the plan's amount on the sheet's today and audits it: claims generated from then on take the
 * new amount; the drafts it already generated are edited to it next ({@code EditPlanDrafts}).
 */
public class ChangePlanAmount {
    public void change(@Val PlanAmount input, @Val Sheet sheet, @Val Plan plan, PlanRepository plans,
            PersonRepository people, HttpServletRequest request, Audit audit, Out<PlanPaused> done) {
        plans.changeAmount(plan.id(), sheet.today(), input.amount());
        var person = people.find(sheet.id(), plan.personId());
        audit.record("PLAN_CHANGED sheet=" + sheet.id() + " by=" + Audit.user(request) + " name="
                + (person == null ? "" : person.getName()) + " start=" + plan.start() + " amount="
                + input.amount().toPlainString());
        done.set(new PlanPaused(plans.find(sheet.id(), plan.id())));
    }
}
