package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Snapshot;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Sets each checked line in order exactly as the rate form would: replacing the grade's rate for the
 *  same from-date, audited RATE_SET and then the figures it moved. */
public class SetImportedRates {
    public void set(@PathVariable("sheetId") String sheetId, @Val ImportedRates imported, @Val Actor actor,
            Rates rates, Sheets sheets, Rollup rollup, Audit audit, Out<ImportRatesResult> result) {
        RecordRateSet record = new RecordRateSet();
        for (int i = 0; i < imported.rates().size(); i++) {
            Rate rate = imported.rates().get(i);
            Snapshot before = rollup.snapshot(sheetId);
            rates.save(rate, sheets.today(sheetId));
            record.record(imported.requests().get(i), rate, audit);
            rollup.record(sheetId, before, actor.user());
        }
        result.set(new ImportRatesResult(imported.rates().size()));
    }
}
