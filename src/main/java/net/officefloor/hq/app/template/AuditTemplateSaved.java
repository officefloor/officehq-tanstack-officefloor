package net.officefloor.hq.app.template;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditTemplateSaved {
    public void audit(@Val ClaimTemplate template, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("TEMPLATE_SAVED", template.sheetId(), Audit.user(connection), "name=" + template.name(),
                "amount=" + template.amount().toPlainString(), "note=" + template.note()));
    }
}
