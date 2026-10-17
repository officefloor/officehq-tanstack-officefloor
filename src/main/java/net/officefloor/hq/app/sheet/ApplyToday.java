package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Sets the sheet's today, starting the sheet when the id was never used. */
public class ApplyToday {
    public void apply(@PathVariable("sheetId") String sheetId, @Val LocalDate today, SheetRepository repository,
            Out<Sheet> sheetOut) {
        String id = sheetId.trim();
        Sheet sheet = repository.findById(id).orElseGet(() -> new Sheet(id));
        sheet.setToday(today);
        sheetOut.set(repository.save(sheet));
    }
}
