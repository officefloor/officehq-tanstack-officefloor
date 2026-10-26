package net.officefloor.hq.app.voidreason;

import net.officefloor.plugin.variable.Val;

public class AddVoidReason {
    public void add(@Val VoidReason reason, VoidReasons reasons) {
        reasons.add(reason.sheetId(), reason.reason());
    }
}
