package net.officefloor.hq.app.user;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The columns of the people list the user has switched off on the sheet (none unless chosen). */
public class ListColumns {
    public void list(@PathVariable("user") String user, @RequestParam(name = "sheet", required = false) String sheet,
            ColumnRepository columns, ObjectResponse<UserColumns> response) {
        String name = user.trim();
        String sheetId = sheet == null ? "" : sheet.trim();
        response.send(new UserColumns(name, sheetId, columns.hiddenBy(sheetId, name)));
    }
}
