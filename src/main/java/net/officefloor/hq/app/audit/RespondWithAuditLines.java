package net.officefloor.hq.app.audit;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's audit lines as the file holds them, newest first; only those about the named person when a name is given. */
public class RespondWithAuditLines {
    public void respond(@Val Sheet sheet, @RequestParam(value = "name", required = false) String name, Audit audit,
            ObjectResponse<List<String>> response) {
        response.send(audit.lines(sheet.id(), name));
    }
}
