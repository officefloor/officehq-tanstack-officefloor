package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads the sheet's period named in the path; refused (400) when there is none. */
public class LoadPeriod {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            PeriodRepository repository, Out<Period> period) throws InvalidPeriodException {
        Period found = repository.find(sheetId, name);
        if (found == null) {
            throw new InvalidPeriodException("No such period: " + name);
        }
        period.set(found);
    }
}
