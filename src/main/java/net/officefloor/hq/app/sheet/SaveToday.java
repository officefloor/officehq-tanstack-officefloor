package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Val;

public class SaveToday {
    public void save(@Val Sheet sheet, @Val LocalDate today, SheetRepository repository) {
        repository.saveToday(sheet.id(), today);
    }
}
