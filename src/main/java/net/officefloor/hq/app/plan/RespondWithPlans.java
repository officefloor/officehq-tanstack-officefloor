package net.officefloor.hq.app.plan;

import java.util.List;
import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every plan on the sheet, in the order they were added, each with its next dates after the sheet's today. */
public class RespondWithPlans {
    public void respond(@PathVariable("sheetId") String sheetId, PlanRepository repository, SheetRepository sheets,
            ObjectResponse<List<PlanResponse>> response) {
        LocalDate today = sheets.today(sheetId.trim());
        response.send(repository.list(sheetId.trim()).stream().map(p -> PlanResponse.from(p, today)).toList());
    }
}
