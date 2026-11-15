package net.officefloor.hq.app.audit;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's audit lines as the audit file holds them, newest first; with a name, only those about
 *  that person (name=/person= it, compared as names are). */
public class ListAudit {
    public void list(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "name", required = false) String name, Audit audit,
            ObjectResponse<List<String>> response) {
        response.send(audit.lines(sheetId.trim(), name));
    }
}
