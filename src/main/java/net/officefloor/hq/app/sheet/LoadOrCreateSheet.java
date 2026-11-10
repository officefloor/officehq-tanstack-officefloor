package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadOrCreateSheet {
    public void load(@PathVariable("sheetId") String sheetId, SheetRepository repository,
            Out<Sheet> sheetOut) throws InvalidSheetIdException {
        String id = sheetId == null ? "" : sheetId.trim();
        if (id.isEmpty()) throw new InvalidSheetIdException();
        sheetOut.set(repository.loadOrCreate(id));
    }
}
