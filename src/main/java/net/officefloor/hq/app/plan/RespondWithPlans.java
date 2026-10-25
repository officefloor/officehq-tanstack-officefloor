package net.officefloor.hq.app.plan;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's plans, in the order they were added. */
public class RespondWithPlans {
    public void respond(@Val Sheet sheet, PlanRepository plans, PlanClaimRepository made, PlanPauseRepository pauses,
            PlanChangeRepository changes,
            ObjectResponse<List<PlanResponse>> response) {
        response.send(plans.findBySheetIdOrderByIdAsc(sheet.getId()).stream()
                .map(p -> PlanResponse.of(p, sheet.getToday(), made, pauses, changes)).toList());
    }
}
