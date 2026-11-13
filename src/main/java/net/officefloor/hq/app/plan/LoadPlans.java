package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadPlans {
    public void load(@Val Sheet sheet, PlanRepository repository, Out<Plans> plans) {
        plans.set(new Plans(repository.findAll(sheet.id())));
    }
}
