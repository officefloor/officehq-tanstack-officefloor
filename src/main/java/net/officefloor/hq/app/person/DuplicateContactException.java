package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetException;

/** A contact already in use by another person on the sheet; refused like any invalid change (400). */
public class DuplicateContactException extends InvalidSheetException {
    public DuplicateContactException(String contact) {
        super("The contact " + contact + " is already in use");
    }
}
