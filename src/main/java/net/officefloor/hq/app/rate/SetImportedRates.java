package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupSnapshot;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Sets the imported rates, in order, each as the rate form sets it: replacing a rate for the same grade and from-date,
 * writing RATE_SET, then the rollup lines it moved.
 */
public class SetImportedRates {

    public void set(@Val SheetResponse sheet, @Val ImportedRates imported, @Val ChangedBy by,
            RateRepository repository, Rollup rollup, Audit audit, Out<ImportRatesResponse> response) {
        for (RateInput input : imported.rates()) {
            RollupSnapshot before = rollup.snapshot(sheet.id());
            Rate rate = repository.set(sheet.id(), input.grade(), input.rate(), input.from(), input.to());
            audit.record(RecordRateSet.line(rate, by));
            rollup.record(sheet.id(), before, by);
        }
        response.set(new ImportRatesResponse(imported.rates().size()));
    }
}
