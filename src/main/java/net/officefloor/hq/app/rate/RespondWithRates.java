package net.officefloor.hq.app.rate;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithRates {
    // The sheet's own rates, then those of each sheet up its parents (see Rates#lineage), each with its level.
    public void respond(@PathVariable("sheetId") String sheetId, Rates rates,
            ObjectResponse<List<RateResponse>> response) {
        List<String> lineage = rates.lineage(sheetId.trim());
        List<RateResponse> all = new ArrayList<>();
        for (int level = 0; level < lineage.size(); level++) {
            int at = level;
            rates.list(lineage.get(level)).forEach(rate -> all.add(RateResponse.from(rate, at)));
        }
        response.send(all);
    }
}
