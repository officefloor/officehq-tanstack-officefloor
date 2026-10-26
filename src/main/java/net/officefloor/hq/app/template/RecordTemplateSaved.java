package net.officefloor.hq.app.template;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordTemplateSaved {
    public void record(@Val Template template, @Val ChangedBy by, Audit audit) {
        audit.record("TEMPLATE_SAVED sheet=" + template.sheetId() + " by=" + by.name() + " name=" + template.name()
                + " amount=" + template.amount().toPlainString() + " note=" + template.note());
    }
}
