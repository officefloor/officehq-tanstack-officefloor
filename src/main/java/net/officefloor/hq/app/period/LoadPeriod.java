package net.officefloor.hq.app.period;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPeriod {
    public void load(@Val SheetResponse sheet, @PathVariable("periodId") String periodId,
            PeriodRepository repository, Out<Period> period) throws BadRequestException {
        Period found = null;
        try {
            found = repository.find(sheet.id(), Long.parseLong(periodId));
        } catch (NumberFormatException e) {
            // not an id: no such period
        }
        if (found == null) {
            throw new BadRequestException("No such period on this sheet");
        }
        period.set(found);
    }
}
