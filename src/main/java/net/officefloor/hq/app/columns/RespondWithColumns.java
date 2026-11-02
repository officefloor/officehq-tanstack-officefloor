package net.officefloor.hq.app.columns;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Responds with the people-list columns the signed-in user switched off on the sheet. */
public class RespondWithColumns {
    public void respond(@PathVariable("sheet") String sheet, @RequestParam("user") String user, Columns columns,
            ObjectResponse<UserColumns> response) {
        response.send(new UserColumns(columns.hidden(sheet, user)));
    }
}
