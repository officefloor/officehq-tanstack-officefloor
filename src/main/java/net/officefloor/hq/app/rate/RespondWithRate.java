package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithRate {
    public void respond(@PathVariable("sheetId") String sheetId, @Val RateInput rate, RateStore store,
            ObjectResponse<RateResponse> response) {
        response.send(RateResponse.of(rate.grade(), rate.pct(), rate.from(), rate.to(),
                store.version(sheetId.trim(), rate.grade(), rate.from())));
    }
}
