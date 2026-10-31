package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithRates {
    public void respond(@PathVariable("sheetId") String sheetId, Rates rates,
            ObjectResponse<List<RateResponse>> response) {
        response.send(rates.onSheet(sheetId).stream().map(RateResponse::from).toList());
    }
}
