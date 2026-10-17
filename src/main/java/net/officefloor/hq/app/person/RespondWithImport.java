package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithImport {
    public void respond(@Val ImportedPeople imported, ObjectResponse<ImportResponse> response) {
        response.send(new ImportResponse(imported.people().size(), List.of()));
    }
}
