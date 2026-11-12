package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Out;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Sets each imported rate in order as the rate form would, recorded as RATE_SET (see {@link AuditRateSet})
 *  with how it moved the sheet's figures. */
public class SaveRateImport {
    public void save(@PathVariable("sheetId") String sheetId, @Val RateImportInput imported, RateStore store, SheetStore sheets,
            Audit audit, RollupService rollups, Out<RateImportResult> result) {
        String sheet = sheetId.trim();
        for (RateInput rate : imported.rates()) {
            Rollup before = rollups.snapshot(sheet);
            store.set(sheet, rate.grade(), rate.from(), rate.to(), rate.pct(), sheets.today(sheet));
            audit.record(AuditRateSet.line(sheet, rate));
            rollups.record(sheet, rate.user(), before);
        }
        result.set(new RateImportResult(imported.rates().size()));
    }
}
