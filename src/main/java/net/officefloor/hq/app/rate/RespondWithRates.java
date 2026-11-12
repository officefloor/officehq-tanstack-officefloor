package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithRates {
    public void respond(@PathVariable("sheetId") String sheetId, RateStore store,
            ObjectResponse<List<RateResponse>> response) {
        response.send(store.list(sheetId.trim()));
    }
}
