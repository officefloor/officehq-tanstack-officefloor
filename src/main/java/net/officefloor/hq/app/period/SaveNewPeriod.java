package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveNewPeriod {
    public void save(@PathVariable("sheetId") String sheetId, @Val PeriodInput input,
            PeriodRepository repository, Out<Period> period) {
        period.set(repository.insert(sheetId.trim(), input.name(), input.from(), input.to()));
    }
}
