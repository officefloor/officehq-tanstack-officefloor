package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Sets each imported rate, in order, exactly as the rate form does: replacing any the grade had for that from-date,
 * recorded as RATE_SET, followed by the ROLLUP and TOTALS lines for the figures it moved.
 */
public class SaveImportedRates {
    public void save(@Val ImportedRates imported, @Val Sheet sheet, @RequestParam("user") String user,
            RateRepository repository, RollupService rollups, Audit audit, Out<ImportRatesResult> result) {
        int set = 0;
        for (RateRequest line : imported.lines()) {
            Rollup before = rollups.compute(sheet.id());
            Rate[] rate = new Rate[1];
            new BuildRate().build(line, sheet, value -> rate[0] = value);
            repository.save(rate[0]);
            new AuditRateSet().audit(rate[0], user, audit);
            RollupLines.record(before, rollups.compute(sheet.id()), sheet.id(), user, audit);
            set++;
        }
        result.set(new ImportRatesResult(set));
    }
}
