package net.officefloor.hq.app.sheet;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The ids of the sheet's children (the sheets naming it as their parent), by id. */
public class RespondWithChildren {
    public void respond(@Val Sheet sheet, SheetRepository repository, ObjectResponse<List<String>> response) {
        response.send(repository.childrenOf(sheet.getId()));
    }
}
