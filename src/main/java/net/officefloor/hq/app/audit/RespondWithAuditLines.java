package net.officefloor.hq.app.audit;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's audit lines exactly as the file holds them, newest first; with a name, only those about that person
 *  (see Audit#lines). Read from the file: the viewer is not a second record. */
public class RespondWithAuditLines {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "name", required = false) String name, Audit audit,
            ObjectResponse<List<String>> response) {
        response.send(audit.lines(sheetId.trim(), name == null ? "" : name));
    }
}
