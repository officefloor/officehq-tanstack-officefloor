package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Finds the sheet, starting an empty one when the id was never used. */
public class LoadSheet {
    public void load(@PathVariable("sheetId") String sheetId, SheetRepository repository, Out<Sheet> sheetOut) {
        String id = sheetId.trim();
        Sheet sheet = repository.findById(id).orElseGet(() -> new Sheet(id));
        sheet.markLoaded();
        sheetOut.set(repository.save(sheet));
    }
}
