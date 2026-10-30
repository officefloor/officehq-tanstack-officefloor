package net.officefloor.hq.app.audit;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's audit lines as the file holds them, newest first; only a person's when a name is given. */
public class LoadAuditLines {
    public void service(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "name", required = false) String name, Audit audit,
            ObjectResponse<List<String>> response) {
        response.send(audit.lines(sheetId, name));
    }
}
