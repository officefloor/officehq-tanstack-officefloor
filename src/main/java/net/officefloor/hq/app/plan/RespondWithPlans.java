package net.officefloor.hq.app.plan;

import java.util.List;
import java.time.LocalDate;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Every plan on the sheet, in the order they were added, each with its next dates after the sheet's today (or the
 *  preview date, when one is given). */
public class RespondWithPlans {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "preview", required = false) String preview, PlanRepository repository, SheetRepository sheets,
            ObjectResponse<List<PlanResponse>> response) {
        LocalDate previewed = AsOf.parse(preview);
        LocalDate today = previewed != null ? previewed : sheets.today(sheetId.trim());
        response.send(repository.list(sheetId.trim()).stream().map(p -> PlanResponse.from(p, today)).toList());
    }
}
