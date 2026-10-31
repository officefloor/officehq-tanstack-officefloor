package net.officefloor.hq.app.template;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the template is saved, so a refused one writes no audit line. */
public class RecordClaimTemplateSaved {
    public void record(@Val ClaimTemplate t, Audit audit) {
        audit.record("TEMPLATE_SAVED sheet=" + t.sheetId() + " by=" + t.by() + " name=" + t.name()
                + " amount=" + t.amount().toPlainString() + " note=" + t.note());
    }
}
