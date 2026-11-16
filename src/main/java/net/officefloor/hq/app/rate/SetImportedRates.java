package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Sets the checked lines in the order pasted, each as the rate form sets one: replacing the grade's rate for the same
 *  from-date (see SaveRate), written as RATE_SET with the figures it moved (see RecordRateSet). */
public class SetImportedRates {
    public void set(@PathVariable("sheetId") String sheetId, @Val ImportedRates imported, RateRepository rates,
            Rollup rollup, Audit audit, Out<ImportRatesResult> result) {
        String sheet = sheetId.trim();
        for (RateValues values : imported.rates()) {
            RollupBefore before = rollup.take(sheet);
            Rate rate = rates.save(new Rate(sheet, values.grade(), values.rate(), values.from(), values.to()));
            new RecordRateSet().record(rate, values, before, rollup, audit);
        }
        result.set(new ImportRatesResult(imported.rates().size()));
    }
}
