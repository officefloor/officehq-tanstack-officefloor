package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads the sheet by id, starting an empty one when the id was never used. */
public class LoadOrCreateSheet {
    public void load(@PathVariable("sheetId") String sheetId, SheetRepository repository, Out<Sheet> sheetOut) {
        String id = sheetId.trim();
        sheetOut.set(repository.findById(id).orElseGet(() -> repository.save(new Sheet(id))));
    }
}
