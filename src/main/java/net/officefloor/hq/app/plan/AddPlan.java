package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the plan to the person. Publishes the plan as stored. */
public class AddPlan {
    public void add(@Val Person person, @Val PlanValues values, PlanRepository plans, Out<Plan> plan) {
        plan.set(plans.add(person.sheetId(), person.id(), values));
    }
}
