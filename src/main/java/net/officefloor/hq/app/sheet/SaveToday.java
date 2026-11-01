package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveToday {
    public void save(@PathVariable("sheet") String sheet, @Val LocalDate today, SheetClock clock) {
        clock.setToday(sheet, today);
    }
}
