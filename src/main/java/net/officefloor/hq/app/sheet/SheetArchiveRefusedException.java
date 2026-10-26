package net.officefloor.hq.app.sheet;

/** An archive asked for by a user who is not a steward of the sheet. */
public class SheetArchiveRefusedException extends Exception {

    public SheetArchiveRefusedException(String message) {
        super(message);
    }
}
