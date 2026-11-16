package net.officefloor.hq.app.sheet;

import java.util.Map;
import net.officefloor.hq.app.pool.PoolRepository;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the clone just made: SHEET_CLONE_MADE on this sheet, then on the clone SHEET_CLONED as its
 *  first line, followed by its ROLLUP line for every person and its TOTALS. */
public class RecordSheetCloned {
    public void record(@RequestParam(name = "by", required = false) String by, @Val Clone clone,
            PersonRepository people, ClaimRepository claims, RateRepository rates, SheetRepository sheets,
            SettingRepository settings, PoolRepository pools, Audit audit) {
        String user = by == null ? "" : by;
        audit.record("SHEET_CLONE_MADE sheet=" + clone.from() + " by=" + user + " to=" + clone.to());
        audit.record("SHEET_CLONED sheet=" + clone.to() + " by=" + user + " from=" + clone.from());
        Rollup nothing = new Rollup(Map.of(), null, null, null, null);
        for (String line : Rollup.of(clone.to(), people, claims, rates, sheets, settings, pools)
                .changesSince(nothing, clone.to(), user)) {
            audit.record(line);
        }
    }
}
