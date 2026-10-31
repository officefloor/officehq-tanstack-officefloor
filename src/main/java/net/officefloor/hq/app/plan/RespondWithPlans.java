package net.officefloor.hq.app.plan;

import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithPlans {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, Plans plans, AsOf asOfSheet,
            ObjectResponse<List<PlanResponse>> response) throws InvalidSheetRequestException {
        response.send(asOfSheet.view(sheetId, asOf,
                () -> plans.onSheet(sheetId).stream().map(PlanResponse::from).toList()));
    }
}
