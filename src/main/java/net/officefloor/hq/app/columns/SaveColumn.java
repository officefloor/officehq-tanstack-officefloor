package net.officefloor.hq.app.columns;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Shows or switches off the column; it is the user's own view of the sheet, so nothing is audited. */
public class SaveColumn {
    public void save(@Val Sheet sheet, @Val ColumnChoice choice, ColumnsRepository repository) {
        repository.show(sheet.id(), choice.user(), choice.column(), choice.shown());
    }
}
