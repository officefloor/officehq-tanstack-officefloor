package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonAliasAdded {
    public void audit(@Val Alias alias, @Val Person loaded, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("PERSON_ALIAS_ADDED", loaded.sheetId(), Audit.user(connection), "name=" + loaded.name(),
                "alias=" + alias.alias()));
    }
}
