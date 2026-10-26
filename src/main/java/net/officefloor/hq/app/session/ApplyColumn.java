package net.officefloor.hq.app.session;

import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;

/** Show or hide the column on the user's screen (no audit line: it changes only how this user's screen shows the list). */
public class ApplyColumn {
    public void apply(@Val SheetResponse sheet, @Val UserResponse user, @Val ColumnChoice choice,
            ColumnsRepository columns) {
        columns.setShown(sheet.id(), user.name(), choice.column(), choice.shown());
    }
}
