package net.officefloor.hq.app.sheet;

/** A change asked of an archived sheet, which changes nothing more. */
public class SheetArchivedException extends Exception {

    public SheetArchivedException(String message) {
        super(message);
    }
}
