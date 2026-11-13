package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;

public class SaveGeneratedPeriods {
    public void save(@Val GeneratedPeriods generated, PeriodRepository repository) {
        generated.periods().forEach(repository::insert);
    }
}
