package net.officefloor.hq.app.importpeople;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithImportedPeople {
    public void respond(@Val ImportedPeople added, ObjectResponse<ImportedPeople> response) {
        response.send(added);
    }
}
