package net.officefloor.hq.app.columns;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Saves the user's choice for one column. Not audited: it changes nothing on the sheet, only what their screen shows. */
public class SaveColumnChoice {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val ColumnChoice choice, Columns columns) {
        columns.set(sheet, user, choice.column(), choice.shown());
    }
}
