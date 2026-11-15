package net.officefloor.hq.app.user;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Switch a column of the people list on or off, for the user on the sheet only; no audit record. */
public class SaveColumn {
    public void save(@PathVariable("user") String user, @PathVariable("column") String column,
            @RequestParam(name = "sheet", required = false) String sheet, @Val ColumnRequest request,
            ColumnRepository columns, ObjectResponse<UserColumns> response) throws InvalidColumnException {
        String name = user.trim();
        String sheetId = sheet == null ? "" : sheet.trim();
        if (name.isEmpty() || sheetId.isEmpty()) {
            throw new InvalidColumnException("Sign in and load a sheet to choose columns.");
        }
        columns.set(sheetId, name, column.trim(), request.shown());
        response.send(new UserColumns(name, sheetId, columns.hiddenBy(sheetId, name)));
    }
}
