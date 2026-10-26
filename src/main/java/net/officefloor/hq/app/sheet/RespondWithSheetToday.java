package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSheetToday {
    public void respond(@Val SheetResponse sheet, SheetRepository repository, RateRepository rates,
            ObjectResponse<SheetTodayResponse> response) {
        response.send(new SheetTodayResponse(sheet.id(), repository.today(sheet.id()).toString(),
                repository.isArchived(sheet.id()), rates.parentOf(sheet.id())));
    }
}
