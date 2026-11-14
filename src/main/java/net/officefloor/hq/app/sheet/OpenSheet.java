package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class OpenSheet {
    public void open(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            SheetRepository repository, Out<Sheet> sheet) {
        sheet.set(repository.open(sheetId.trim(), by == null ? "" : by));
    }
}
