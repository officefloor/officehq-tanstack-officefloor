package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadRates {
    public void load(@Val Sheet sheet, RateRepository repository, Out<List<Rate>> rates) {
        rates.set(repository.findAll(sheet.id()));
    }
}
