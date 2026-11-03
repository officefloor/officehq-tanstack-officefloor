package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSheetExists {
    public void respond(@Val SheetExists exists, ObjectResponse<SheetExists> response) {
        response.send(exists);
    }
}
