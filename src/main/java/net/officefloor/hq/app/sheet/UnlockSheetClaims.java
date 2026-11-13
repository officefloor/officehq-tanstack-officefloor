package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.claim.ClaimLockRepository;
import net.officefloor.plugin.variable.Val;

/** Releases every claim lock on the sheet: a change of the sheet's today closes every open claim editor. */
public class UnlockSheetClaims {
    public void unlock(@Val Sheet sheet, ClaimLockRepository locks) {
        locks.unlockAll(sheet.id());
    }
}
