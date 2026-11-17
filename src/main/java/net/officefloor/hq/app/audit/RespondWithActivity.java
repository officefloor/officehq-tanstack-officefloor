package net.officefloor.hq.app.audit;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's activity counts read from the audit file (see Audit#activity). */
public class RespondWithActivity {
    public void respond(@PathVariable("sheetId") String sheetId, Audit audit,
            ObjectResponse<List<String>> response) {
        response.send(audit.activity(sheetId.trim()));
    }
}
