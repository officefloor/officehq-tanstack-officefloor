package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

public class AddPlan {
    public void add(@RequestParam(name = "by", required = false) String by, @Val ValidPlan request,
            @Val Person person, PlanRepository plans, Out<Plan> plan) {
        plan.set(plans.add(person.getSheetId(), person.getId(), request, by == null ? "" : by));
    }
}
