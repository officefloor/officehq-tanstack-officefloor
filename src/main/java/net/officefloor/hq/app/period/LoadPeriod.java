package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads the sheet's period by name, refusing a name not on the sheet. */
public class LoadPeriod {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            PeriodRepository repository, Out<Period> periodOut) throws InvalidPeriodException {
        periodOut.set(repository.findBySheetId(sheetId.trim()).stream().filter((p) -> p.getName().equals(name))
                .findFirst().orElseThrow(() -> new InvalidPeriodException("No period named " + name + " on the sheet.")));
    }
}
