package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class ApplyToday {
    public void apply(@Parameter LocalDate today, @PathVariable("sheetId") String sheetId, Sheets sheets,
            Out<Sheet> sheetOut) {
        Sheet sheet = sheets.find(sheetId.trim()).withToday(today);
        sheets.save(sheet);
        sheetOut.set(sheet);
    }
}
