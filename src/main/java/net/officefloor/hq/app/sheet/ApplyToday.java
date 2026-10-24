package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Val;

public class ApplyToday {
    public void apply(@Val LocalDate today, @Val Sheet sheet, SheetRepository repository) {
        sheet.setToday(today);
        repository.save(sheet);
    }
}
