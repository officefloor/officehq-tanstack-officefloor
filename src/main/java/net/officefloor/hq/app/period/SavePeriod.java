package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds the period, refusing a name already on the sheet or dates that overlap another period. */
public class SavePeriod {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val PeriodInput input,
            Periods periods, Audit audit) throws InvalidPeriodException {
        if (periods.named(sheet, input.name())) {
            throw new InvalidPeriodException("There is already a period named " + input.name());
        }
        if (periods.overlaps(sheet, input)) {
            throw new InvalidPeriodException("A period may not overlap another period");
        }
        periods.add(sheet, input);
        audit.record("PERIOD_ADDED sheet=" + sheet + " by=" + user + " period=" + input.name() + " from="
                + input.from() + " to=" + input.to() + (input.cap() == null ? "" : " cap=" + input.cap()));
    }
}
