package net.officefloor.hq.app.template;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the template, once it is saved. */
public class AuditTemplateSaved {
    public void audit(@Val ClaimTemplate template, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("TEMPLATE_SAVED sheet=" + template.getSheetId() + " by=" + AuditUser.decode(user)
                + " name=" + template.getName() + " amount=" + template.getAmount().toPlainString()
                + " note=" + template.getNote());
    }
}
