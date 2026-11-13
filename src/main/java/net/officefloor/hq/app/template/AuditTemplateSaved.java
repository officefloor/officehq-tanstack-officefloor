package net.officefloor.hq.app.template;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the claim template saved on the sheet. */
public class AuditTemplateSaved {
    public void audit(@Val ClaimTemplate template, @RequestParam("user") String user, Audit audit) {
        audit.record("TEMPLATE_SAVED sheet=" + template.sheetId() + " by=" + user + " name=" + template.name()
                + " amount=" + template.amount().setScale(2).toPlainString() + " note=" + template.note());
    }
}
