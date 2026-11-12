package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Clones the sheet to the other id (see {@link SheetCloneStore}). */
public class CloneSheet {
    public void clone(@PathVariable("sheetId") String sheetId, @Val CloneRequest request, SheetCloneStore store)
            throws InvalidSheetException {
        // checked again inside the transaction: another clone or load may have used the id since
        store.cloneTo(sheetId.trim(), request.to());
    }
}
