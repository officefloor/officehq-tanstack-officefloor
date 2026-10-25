package net.officefloor.hq.app.opening;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithImportedOpenings {
    public void respond(@Val ImportedOpenings added, ObjectResponse<ImportedOpenings> response) {
        response.send(added);
    }
}
