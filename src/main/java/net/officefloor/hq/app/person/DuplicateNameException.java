package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetException;

/** A name already in use by another person on the sheet; refused like any invalid change (400). */
public class DuplicateNameException extends InvalidSheetException {
    public DuplicateNameException(String name) {
        super("The name " + name + " is already in use");
    }
}
