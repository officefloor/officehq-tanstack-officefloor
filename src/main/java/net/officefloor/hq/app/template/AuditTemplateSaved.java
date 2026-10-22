package net.officefloor.hq.app.template;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored template in the audit file, with its amount as the screen shows it. */
public class AuditTemplateSaved {
    public void audit(@Val Template t, Audit audit) {
        audit.record("TEMPLATE_SAVED sheet=" + t.sheetId() + " by=" + t.by() + " name=" + t.name()
                + " amount=" + TemplateResponse.from(t).amount() + " note=" + t.note());
    }
}
