package net.officefloor.hq.app.rate;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's rates, by grade. */
public class RespondWithRates {
    public void respond(@PathVariable("sheetId") String sheetId, RateRepository repository,
            ObjectResponse<RatesResponse> response) {
        response.send(RatesResponse.from(repository.findBySheetIdOrdered(sheetId.trim())));
    }
}
