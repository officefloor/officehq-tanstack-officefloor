package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored change (PLAN_CHANGED) in the audit file, naming the plan by its person and start. */
public class AuditPlanAmount {
    public void audit(@Val PlanAmountInput input, @Val Plan loaded, PersonRepository people, Audit audit) {
        var person = people.find(loaded.sheetId(), loaded.personId());
        audit.record("PLAN_CHANGED sheet=" + loaded.sheetId() + " by=" + input.user() + " name=" + person.name()
                + " start=" + loaded.start() + " amount=" + input.amount().toPlainString());
    }
}
