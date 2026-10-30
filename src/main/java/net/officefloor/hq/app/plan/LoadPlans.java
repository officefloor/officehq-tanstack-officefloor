package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's plans, in the order they were added, each with its next dates after the sheet's today. */
public class LoadPlans {
    public void service(@PathVariable("sheetId") String sheetId, Plans plans, Sheets sheets, ObjectResponse<List<PlanResponse>> response) {
        LocalDate today = sheets.today(sheetId);
        response.send(plans.list(sheetId).stream().map(p -> PlanResponse.from(p, today)).toList());
    }
}
