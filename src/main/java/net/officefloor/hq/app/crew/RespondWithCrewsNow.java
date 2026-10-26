package net.officefloor.hq.app.crew;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's crews as they stand once a change is made. */
public class RespondWithCrewsNow {
    public void respond(@Val SheetResponse sheet, CrewRepository repository, ObjectResponse<List<CrewResponse>> response) {
        response.send(repository.list(sheet.id()));
    }
}
