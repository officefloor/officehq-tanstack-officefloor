package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRates {
    public void respond(@Val SheetResponse sheet, RateRepository repository,
            ObjectResponse<List<RateResponse>> response) {
        response.send(repository.list(sheet.id()).stream().map(RateResponse::from).toList());
    }
}
