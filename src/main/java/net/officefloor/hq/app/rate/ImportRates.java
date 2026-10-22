package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Set each imported rate in order just as the rate form would: replacing any the grade had from that same date,
 *  writing RATE_SET, then how the sheet's figures moved over it. */
public class ImportRates {
    public void importRates(@PathVariable("sheetId") String sheetId, @Val RateImportInput input,
            RateRepository repository, Rollups rollups, Audit audit, Out<RateImportResponse> result) {
        String sheet = sheetId.trim();
        for (RateInput rate : input.rates()) {
            Rollup before = rollups.take(sheet);
            Rate saved = repository.set(sheet, rate.grade(), rate.pct(), rate.from(), rate.to());
            RateRules.record(rate, saved, audit);
            rollups.record(sheet, input.user(), before);
        }
        result.set(new RateImportResponse(input.rates().size()));
    }
}
