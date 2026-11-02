package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Sets each checked line, in order, exactly as the rate form's SaveRate does. */
public class SetImportedRates {
    public void set(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val ImportedRates imported, Rates rates, Audit audit, Rollup rollup,
            ObjectResponse<RateImportResponse> response) {
        for (RateInput input : imported.lines()) {
            SaveRate.apply(sheet, user, input, rates, audit, rollup);
        }
        response.send(new RateImportResponse(imported.lines().size(), rates.list(sheet)));
    }
}
