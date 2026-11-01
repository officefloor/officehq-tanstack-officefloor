package net.officefloor.hq.app.rate;

import java.math.RoundingMode;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class SaveRate {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val RateInput input,
            Rates rates, Audit audit, Rollup rollup) {
        Rollup.Snapshot before = rollup.take(sheet);
        rates.set(sheet, input);
        audit.record("RATE_SET sheet=" + sheet + " by=" + user + " grade=" + input.grade() + " rate="
                + input.pct().setScale(2, RoundingMode.UNNECESSARY).toPlainString()
                + (input.from() == null ? "" : " from=" + input.from()));
        rollup.record(sheet, user, before);
    }
}
