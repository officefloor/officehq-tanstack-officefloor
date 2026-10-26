package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplyRate {
    public void apply(@Val SheetResponse sheet, @Val RateInput input, RateRepository repository, SheetRepository sheets,
            Out<Rate> rate) {
        rate.set(repository.set(sheet.id(), input.grade(), input.rate(), input.from(), input.to(),
                sheets.today(sheet.id())));
    }
}
