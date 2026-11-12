package net.officefloor.hq.app.template;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a template saved: TEMPLATE_SAVED sheet=<id> by=<user> name=<name> amount=<0.00> note=<note>. */
public class AuditTemplateSaved {
    public void audit(@PathVariable("sheetId") String sheetId, @Val TemplateInput input, Audit audit) {
        Template t = input.template();
        audit.record("TEMPLATE_SAVED sheet=" + sheetId.trim() + " by=" + input.user() + " name=" + t.name()
                + " amount=" + t.amount().toPlainString() + " note=" + t.note());
    }
}
