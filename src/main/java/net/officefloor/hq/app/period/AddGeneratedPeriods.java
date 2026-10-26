package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;

public class AddGeneratedPeriods {
    public void add(@Val GeneratedPeriods generated, Periods periods) {
        generated.periods().forEach(periods::add);
    }
}
