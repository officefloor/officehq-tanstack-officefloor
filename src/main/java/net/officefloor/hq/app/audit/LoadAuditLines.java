package net.officefloor.hq.app.audit;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class LoadAuditLines {
    public void load(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "name", required = false) String name, ServerHttpConnection connection,
            Audit audit, Out<AuditLines> lines) throws BadRequestException {
        CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        lines.set(new AuditLines(audit.read(id, name)));
    }
}
