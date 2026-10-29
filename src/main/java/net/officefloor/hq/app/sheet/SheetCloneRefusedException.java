package net.officefloor.hq.app.sheet;

/** A clone asked for by a user who is not a steward of the sheet, or to a sheet id that is already used. */
public class SheetCloneRefusedException extends Exception {

    public SheetCloneRefusedException(String message) {
        super(message);
    }
}
