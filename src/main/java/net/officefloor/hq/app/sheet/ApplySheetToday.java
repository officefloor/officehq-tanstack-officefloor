package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Val;

public class ApplySheetToday {
    public void apply(@Val SheetResponse sheet, @Val LocalDate today, SheetRepository repository) {
        repository.setToday(sheet.id(), today);
    }
}
