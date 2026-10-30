package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadRates {
    public void service(@PathVariable("sheetId") String sheetId, Rates rates,
            ObjectResponse<List<RateResponse>> response) {
        response.send(rates.list(sheetId));
    }
}
