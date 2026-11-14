package net.officefloor.hq.app.rollover;

import net.officefloor.hq.app.period.GeneratedPeriods;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Stores the rollover with the periods it generated and the sheet-today it was made on, so it can be undone. */
public class RecordRollover {
    public void record(@Val Rollover rollover, @Val GeneratedPeriods generated, @Val Sheet sheet,
            @RequestParam("user") String user, RolloverRepository repository, Out<RolloverId> id) {
        id.set(new RolloverId(repository.insert(sheet.id(), rollover.fy(), user, sheet.today(),
                generated.periods().stream().map(Period::name).toList())));
    }
}
