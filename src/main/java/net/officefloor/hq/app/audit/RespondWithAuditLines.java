package net.officefloor.hq.app.audit;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithAuditLines {
    public void respond(@Val AuditLines lines, ObjectResponse<AuditLines> response) {
        response.send(lines);
    }
}
