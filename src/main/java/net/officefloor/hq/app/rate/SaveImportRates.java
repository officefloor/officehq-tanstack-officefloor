package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Sets the imported rates in order, each exactly as the rate form would: RATE_SET, then the figures it moved. */
public class SaveImportRates {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidImportRates request, Rates rates, Audit audit, Rollup rollup,
            ObjectResponse<ImportRatesResponse> response) {
        for (ValidRate rate : request.rates()) {
            RollupFigures before = rollup.figures(sheetId);
            SetRate.apply(sheetId, rate, rates, audit);
            rollup.recordChanges(sheetId, request.user(), before);
        }
        response.send(new ImportRatesResponse(request.rates().size()));
    }
}
