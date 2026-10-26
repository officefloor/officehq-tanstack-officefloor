package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPlanAdded {
    public void record(@Val Plan plan, @Val Person person, @Val ChangedBy by, Audit audit) {
        audit.record("PLAN_ADDED sheet=" + plan.sheetId() + " by=" + by.name() + " name=" + person.getName()
                + " amount=" + plan.amount().toPlainString() + " start=" + plan.start() + " every=" + plan.every()
                + " end=" + plan.end());
    }
}
