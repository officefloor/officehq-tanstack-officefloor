package net.officefloor.hq.app;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithAuditLines {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "name", required = false) String name, Audit audit,
            ObjectResponse<List<String>> response) {
        response.send(audit.linesFor(sheetId, name));
    }
}
