package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Pauses the plan from the sheet's today, audited as PLAN_PAUSED; a plan already paused is refused. */
public class PausePlan {
    public void pause(@PathVariable("sheetId") String sheetId, @PathVariable("planId") String planId, @RequestBody PauseRequest request,
            Plans plans, People people, Sheets sheets, Settings settings, Audit audit, ObjectResponse<PlanResponse> response)
            throws InvalidPlanException, ViewerRefusedException {
        PlanPausing.change(sheetId, planId, request, plans, people, sheets, settings, audit, response, true);
    }
}
