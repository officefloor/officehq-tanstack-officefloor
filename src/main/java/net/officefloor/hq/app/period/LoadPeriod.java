package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPeriod {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            PeriodRepository repository, Out<Period> periodOut) throws InvalidPeriodException {
        Period period = repository.findByName(sheetId, name);
        if (period == null) throw new InvalidPeriodException("No such period: " + name);
        periodOut.set(period);
    }
}
