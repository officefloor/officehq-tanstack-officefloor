package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Resumes the paused plan from the sheet's today and audits it; refused (nothing written) when it is running. */
public class ResumePlan {
    public void resume(@Val Plan plan, @Val Actor actor, Plans plans, People people, Sheets sheets, Audit audit)
            throws InvalidSheetRequestException {
        if (!plan.paused()) throw new InvalidSheetRequestException("The plan is not paused");
        plans.resume(plan.id(), sheets.today(plan.sheetId()));
        Person person = people.find(plan.sheetId(), plan.personId());
        audit.record("PLAN_RESUMED sheet=" + plan.sheetId() + " by=" + actor.user() + " name=" + person.getName()
                + " start=" + plan.start());
    }
}
