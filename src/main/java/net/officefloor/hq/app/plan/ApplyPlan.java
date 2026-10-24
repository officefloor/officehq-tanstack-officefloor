package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the plan to the person. */
public class ApplyPlan {
    public void apply(@Val PlanInput input, @Val Person person, PlanRepository plans, Out<Plan> planOut) {
        planOut.set(plans.saveAndFlush(new Plan(person.getSheetId(), person.getId(), input.amount(), input.start(),
                input.every(), input.end())));
    }
}
