package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;

public class AddPeriod {
    public void add(@Val Period period, Periods periods) {
        periods.add(period);
    }
}
