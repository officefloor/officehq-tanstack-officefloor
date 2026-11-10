package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadSheet {
    public void load(@PathVariable("sheetId") String sheetId, SheetRepository repository,
            Out<Sheet> sheetOut) throws SheetNotFoundException {
        Sheet sheet = repository.find(sheetId == null ? "" : sheetId.trim());
        if (sheet == null) throw new SheetNotFoundException(sheetId);
        sheetOut.set(sheet);
    }
}
