package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListRates {
    public void list(@PathVariable("sheetId") String sheetId, RateRepository rates,
            ObjectResponse<List<Rate>> response) {
        response.send(rates.list(sheetId.trim()));
    }
}
