package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.pool.PoolRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Sets each imported line in order exactly as the rate form sets it: replacing the rate for the same
 *  grade and from-date, recorded as RATE_SET, then the ROLLUP and TOTALS lines of the figures it moved. */
public class ImportRates {
    public void importRates(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val ImportRateLines lines,
            SheetRepository sheets, RateRepository rates, PersonRepository people, ClaimRepository claims,
            SettingRepository settings, PoolRepository pools, Audit audit, Out<ImportRatesResult> result) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        sheets.open(id, user);
        for (ValidRate line : lines.lines()) {
            Rollup before = Rollup.of(id, people, claims, rates, sheets, settings, pools);
            Rate rate = rates.set(id, line.grade(), line.pct(), line.from(), line.to(), user);
            audit.record("RATE_SET sheet=" + id + " by=" + user + " grade=" + rate.grade() + " rate=" + rate.pct()
                    + (rate.from() == null ? "" : " from=" + rate.from())
                    + (rate.to() == null ? "" : " to=" + rate.to()));
            Rollup after = Rollup.of(id, people, claims, rates, sheets, settings, pools);
            for (String change : after.changesSince(before, id, user)) {
                audit.record(change);
            }
        }
        result.set(new ImportRatesResult(lines.lines().size()));
    }
}
