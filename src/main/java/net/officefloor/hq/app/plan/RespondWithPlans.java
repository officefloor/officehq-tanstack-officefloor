package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPlans {
    public void respond(@PathVariable("sheetId") String sheetId, Plans plans, Sheets sheets, ObjectResponse<List<PlanResponse>> response) {
        String sheet = sheetId.trim();
        Map<Long, Integer> made = plans.madeCounts(sheet);
        LocalDate today = sheets.today(sheet);
        response.send(plans.list(sheet).stream().map(p -> PlanResponse.from(p, made.getOrDefault(p.id(), 0), today, plans.pauses(p.id()), plans.changes(p.id()))).toList());
    }
}
