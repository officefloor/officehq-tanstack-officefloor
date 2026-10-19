package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithRates {
    public void respond(@PathVariable("sheetId") String sheetId, RateRepository repository,
            ObjectResponse<List<RateResponse>> response) {
        response.send(repository.findBySheet(sheetId).stream().map(RateResponse::from).toList());
    }
}
