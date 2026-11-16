package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's rates, by grade. */
public class RespondWithRates {
    public void respond(@PathVariable("sheetId") String sheetId, RateRepository rates,
            ObjectResponse<List<RateResponse>> response) {
        response.send(rates.list(sheetId.trim()).stream().map(RateResponse::from).toList());
    }
}
