package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class ApplyToday {
    public void apply(@PathVariable("sheetId") String sheetId, @Val LocalDate today, Sheets sheets) {
        sheets.setToday(sheetId, today);
    }
}
