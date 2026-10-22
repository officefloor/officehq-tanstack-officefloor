package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveToday {
    public void save(@PathVariable("sheetId") String sheetId, @Val LocalDate today, SheetRepository repository) {
        repository.setToday(sheetId.trim(), today);
    }
}
