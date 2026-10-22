package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored pool in the audit file. */
public class AuditPoolAdded {
    public void audit(@Val Pool p, Audit audit) {
        audit.record("POOL_ADDED sheet=" + p.sheetId() + " by=" + p.by() + " name=" + p.name());
    }
}
