package net.officefloor.hq.app.audit;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's activity: one line "<user> <EVENT> <count>" for every user and event in its audit lines, sorted by user then event. */
public class RespondWithActivity {
    public void respond(@Val Sheet sheet, Audit audit, ObjectResponse<List<String>> response) {
        response.send(audit.activity(sheet.id()));
    }
}
