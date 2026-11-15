package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds the plan to the person; a sheet named on it that is not a descendant of the person's sheet is refused. */
public class AddPlan {
    public void add(@RequestParam(name = "by", required = false) String by, @Val ValidPlan request,
            @Val Person person, PlanRepository plans, SheetRepository sheets, Out<Plan> plan)
            throws InvalidPlanException {
        if (request.sheet() != null && !sheets.descendants(person.getSheetId()).contains(request.sheet())) {
            throw new InvalidPlanException("Not a descendant sheet: " + request.sheet());
        }
        plan.set(plans.add(person.getSheetId(), person.getId(), request, by == null ? "" : by));
    }
}
