package net.officefloor.hq.app.period;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPeriod {
    public void load(@PathVariable("name") String name, @Val Sheet sheet, PeriodRepository repository,
            Out<Period> periodOut) throws InvalidPeriodException {
        Period period = repository.find(sheet.id(), name == null ? "" : name);
        if (period == null) throw new InvalidPeriodException("There is no period named " + name);
        periodOut.set(period);
    }
}
