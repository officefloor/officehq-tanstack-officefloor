package net.officefloor.hq.app.sheet;

import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.plugin.variable.Val;

public class SaveSheet {
    public void save(@PathVariable("sheetId") String sheetId, @Val SheetRequest request,
            SheetRepository repository) {
        repository.ensure(sheetId.trim(), request.user().trim());
    }
}
