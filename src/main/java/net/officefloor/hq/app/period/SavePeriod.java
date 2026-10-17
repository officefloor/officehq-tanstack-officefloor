package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;

public class SavePeriod {
    public void save(@Val Period period, PeriodRepository repository) {
        repository.save(period);
    }
}
