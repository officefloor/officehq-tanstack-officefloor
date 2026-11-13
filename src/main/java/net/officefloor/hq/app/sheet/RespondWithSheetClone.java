package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Answers with the id of the sheet just cloned to. */
public class RespondWithSheetClone {
    public void respond(@Val SheetClone clone, ObjectResponse<SheetCloneRequest> response) {
        response.send(new SheetCloneRequest(clone.to()));
    }
}
