package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class SavePlan {
    public void save(@Val PlanInput input, @Val Person person, PlanRepository repository, Out<Plan> plan) {
        plan.set(repository.insert(person, input));
    }
}
