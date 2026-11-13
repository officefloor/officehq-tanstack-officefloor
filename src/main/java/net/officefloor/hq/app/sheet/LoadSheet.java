package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadSheet {
    public void load(@PathVariable("sheetId") String sheetId, SheetRepository repository, Out<Sheet> sheet)
            throws SheetNotFoundException {
        Sheet found = repository.find(sheetId);
        if (found == null) {
            throw new SheetNotFoundException(sheetId);
        }
        sheet.set(found);
    }
}
