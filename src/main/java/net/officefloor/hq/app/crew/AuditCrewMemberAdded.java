package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditCrewMemberAdded {
    public void audit(@Val CrewMember member, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("CREW_MEMBER_ADDED", member.sheetId(), Audit.user(connection), "crew=" + member.crew(),
                "name=" + member.name()));
    }
}
