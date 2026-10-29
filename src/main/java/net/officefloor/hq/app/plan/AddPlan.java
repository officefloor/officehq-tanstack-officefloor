package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class AddPlan {
    public void add(@Val PlanRequestChecked checked, @Val Person person, Plans plans, Out<Plan> planOut) {
        planOut.set(plans.add(new Plan(0, person.sheetId(), person.id(), checked.amount(), checked.start(),
                checked.every(), checked.end())));
    }
}
