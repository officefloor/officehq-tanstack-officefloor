package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Every plan on the sheet in the order added; read as of a date (see AsOf), as they stood then. */
public class RespondWithPlans {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, PlanRepository plans,
            ObjectResponse<List<Plan>> response) {
        LocalDate date = AsOf.parse(asOf);
        response.send(plans.list(sheetId.trim(), date));
    }
}
