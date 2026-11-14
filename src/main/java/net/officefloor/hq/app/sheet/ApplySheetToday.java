package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class ApplySheetToday {
    public void apply(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val LocalDate today, SheetRepository repository, Out<Sheet> sheet) {
        String id = sheetId.trim();
        repository.open(id, by == null ? "" : by);
        repository.setToday(id, today);
        sheet.set(repository.find(id));
    }
}
