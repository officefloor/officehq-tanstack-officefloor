package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRates {
    public void respond(@Val Sheet sheet, RateRepository repository, ObjectResponse<List<RateResponse>> response) {
        response.send(repository.findBySheetIdOrderByGradeAscFromDateAscIdAsc(sheet.getId()).stream().map(RateResponse::from).toList());
    }
}
