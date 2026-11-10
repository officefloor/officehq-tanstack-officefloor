package net.officefloor.hq.app.template;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records the template saved. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditTemplateSaved {
    public void audit(@Val ClaimTemplate template, HttpServletRequest request, Audit audit) {
        audit.record("TEMPLATE_SAVED sheet=" + template.sheetId() + " by=" + Audit.user(request)
                + " name=" + template.name() + " amount=" + template.amount().toPlainString()
                + " note=" + template.note());
    }
}
