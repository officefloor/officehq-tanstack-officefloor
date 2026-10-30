package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveRate {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidRate rate, Rates rates, Audit audit, @Val RollupFigures before, Rollup rollup,
            ObjectResponse<List<RateResponse>> response) {
        SetRate.apply(sheetId, rate, rates, audit);
        rollup.recordChanges(sheetId, rate.user(), before);
        response.send(rates.list(sheetId));
    }

}
