package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Refuses to undo the transfer of a person who did not arrive by transfer, or who arrived on a sheet-today other
 *  than this sheet's today; otherwise sets up the transfer back to the sheet they came from. */
public class PrepareTransferUndo {
    public void prepare(@Val TransferUndoRequest request, @Val Person person, SheetStore sheets,
            Out<TransferRequest> transfer) throws InvalidSheetException {
        if (person.getOrigin() == null || person.getArrivedOn() == null) {
            throw new InvalidSheetException(person.getName() + " did not arrive by transfer");
        }
        if (!person.getArrivedOn().equals(sheets.today(person.getSheetId()))) {
            throw new InvalidSheetException("A transfer can be undone only on the day it arrived");
        }
        transfer.set(new TransferRequest(person.getOrigin(), request.user()));
    }
}
