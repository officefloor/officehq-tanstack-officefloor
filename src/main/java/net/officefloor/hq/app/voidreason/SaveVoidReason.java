package net.officefloor.hq.app.voidreason;

import net.officefloor.plugin.variable.Val;

public class SaveVoidReason {
    public void save(@Val VoidReason reason, VoidReasons reasons) {
        reasons.add(reason.sheetId(), reason.reason());
    }
}
