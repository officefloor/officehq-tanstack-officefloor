package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveSheetToday {
    public void save(@PathVariable("sheetId") String sheetId, @Val LocalDate today, SheetClock clock) {
        clock.setToday(sheetId, today);
    }
}
