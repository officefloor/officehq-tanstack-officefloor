package net.officefloor.hq.app.columns;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

public class LoadColumns {
    public void load(@Val Sheet sheet, @RequestParam("user") String user, ColumnsRepository repository,
            Out<UserColumns> columns) {
        columns.set(new UserColumns(user, repository.hidden(sheet.id(), user)));
    }
}
