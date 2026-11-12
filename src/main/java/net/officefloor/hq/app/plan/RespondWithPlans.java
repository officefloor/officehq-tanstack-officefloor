package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPlans {
    public void respond(@PathVariable("sheetId") String sheetId, PlanStore store, SheetStore sheets,
            ObjectResponse<List<PlanResponse>> response) {
        String sheet = sheetId.trim();
        LocalDate today = sheets.today(sheet);
        response.send(store.list(sheet).stream().map(p -> PlanResponse.from(p, today)).toList());
    }
}
