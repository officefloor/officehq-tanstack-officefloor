package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;

/** Shared by pause and resume: finds the sheet's plan, refuses a change to the state it is already in, records and audits it. */
final class PlanPausing {
    static void change(String sheetId, String planId, PauseRequest request, Plans plans, People people, Sheets sheets, Settings settings,
            Audit audit, ObjectResponse<PlanResponse> response, boolean pause) throws InvalidPlanException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        Plan plan;
        try {
            plan = plans.find(Long.parseLong(planId));
        } catch (NumberFormatException e) {
            plan = null;
        }
        if (plan == null || !plan.sheetId().equals(sheetId)) {
            throw new InvalidPlanException("No such plan");
        }
        if (plan.paused() == pause) {
            throw new InvalidPlanException(pause ? "The plan is already paused" : "The plan is not paused");
        }
        Person person = people.find(sheetId, plan.personId());
        LocalDate today = sheets.today(sheetId);
        if (pause) {
            plans.pause(plan.id(), today);
        } else {
            plans.resume(plan.id(), today);
        }
        audit.recordOnCommit((pause ? "PLAN_PAUSED" : "PLAN_RESUMED") + " sheet=" + sheetId + " by=" + request.user()
                + " name=" + (person != null ? person.getName() : "") + " start=" + plan.start());
        response.send(PlanResponse.from(plans.find(plan.id()), today));
    }
}
