package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's rate for each grade that has one. */
public class RespondWithRates {
    public void respond(@PathVariable("sheetId") String sheetId, RateRepository repository,
            ObjectResponse<List<RateResponse>> response) {
        response.send(repository.list(sheetId.trim()).stream().map(RateResponse::from).toList());
    }
}
