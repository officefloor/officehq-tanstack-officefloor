package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveRate {
    public void save(@PathVariable("sheetId") String sheetId, @Val RateInput input,
            RateRepository repository, Out<Rate> rate) {
        rate.set(repository.set(sheetId.trim(), input.grade(), input.pct(), input.from()));
    }
}
