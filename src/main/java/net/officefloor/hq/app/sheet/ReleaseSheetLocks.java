package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.claim.ClaimLocks;
import org.springframework.web.bind.annotation.PathVariable;

public class ReleaseSheetLocks {
    // A change of the sheet's today releases every claim's lock: open editors are closed.
    public void release(@PathVariable("sheetId") String sheetId, ClaimLocks locks) {
        locks.releaseAll(sheetId.trim());
    }
}
