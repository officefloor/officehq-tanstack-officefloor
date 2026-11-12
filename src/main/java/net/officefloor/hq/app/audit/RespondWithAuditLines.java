package net.officefloor.hq.app.audit;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's audit lines as the audit file holds them, newest first, optionally only those about a person. */
public class RespondWithAuditLines {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "person", required = false) String person, Audit audit,
            ObjectResponse<List<String>> response) {
        response.send(audit.lines(sheetId.trim(), person));
    }
}
