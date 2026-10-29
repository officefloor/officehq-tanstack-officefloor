package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditPersonUnretired {
    public void audit(@Val Person loaded, @Val PersonResponse unretired, ServerHttpConnection connection, Audit audit) {
        PersonResponse.Retired past = unretired.retirements().get(unretired.retirements().size() - 1);
        audit.record(Audit.line("PERSON_UNRETIRED", loaded.sheetId(), Audit.user(connection), "name=" + unretired.name(),
                "on=" + past.to()));
    }
}
