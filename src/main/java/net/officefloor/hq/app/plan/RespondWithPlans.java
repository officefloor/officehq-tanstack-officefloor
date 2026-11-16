package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Every plan on the sheet in the order added, each with its next dates after the sheet's today (see PlanView); read
 *  as of a date (see AsOf), as they stood then. */
public class RespondWithPlans {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, PlanRepository plans, SheetRepository sheets,
            ObjectResponse<List<PlanView>> response) {
        String sheet = sheetId.trim();
        LocalDate date = AsOf.parse(asOf);
        LocalDate today = sheets.today(sheet);
        response.send(plans.list(sheet, date).stream().map((p) -> PlanView.of(p, today)).toList());
    }
}
