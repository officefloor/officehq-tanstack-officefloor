package net.officefloor.hq.app.template;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Writes TEMPLATE_SAVED to the audit file once the template is stored. */
public class RecordTemplateSaved {
    public void record(@PathVariable("sheetId") String sheetId, @Val TemplateValues values, Audit audit) {
        Template t = values.template();
        audit.record("TEMPLATE_SAVED sheet=" + sheetId.trim() + " by=" + values.by() + " name=" + t.name()
                + " amount=" + t.amount().toPlainString() + " note=" + t.note());
    }
}
