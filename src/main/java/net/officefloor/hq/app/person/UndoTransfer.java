package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Undoes the transfer that brought the person to this sheet, by the rules of {@link PersonTransferrer}. */
public class UndoTransfer {
    public void undo(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            PersonTransferrer transferrer) throws InvalidPersonException {
        transferrer.undo(sheet, user, person);
    }
}
