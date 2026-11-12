package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the sheet's today; a new today releases every claim's lock (each open editor's). */
public class SaveToday {
    public void save(@PathVariable("sheetId") String sheetId, @Val LocalDate today, SheetStore store, ClaimStore claims) {
        String id = sheetId.trim();
        if (!today.equals(store.today(id))) {
            claims.unlockAll(id);
        }
        store.setToday(id, today);
    }
}
