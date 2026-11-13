package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class SavePlan {
    public void save(@Val Plan plan, PlanRepository plans, Out<Plans> saved) {
        saved.set(new Plans(java.util.List.of(plans.save(plan))));
    }
}
