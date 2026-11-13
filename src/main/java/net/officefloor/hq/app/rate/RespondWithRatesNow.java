package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's rates as they are now, after a change. */
public class RespondWithRatesNow {
    public void respond(@PathVariable("sheetId") String sheetId, RateStore store,
            ObjectResponse<List<RateResponse>> response) {
        response.send(store.list(sheetId.trim()));
    }
}
