package net.officefloor.hq.app.sheet;

/** A change asked for on an archived sheet, which can no longer be changed. */
public class SheetArchivedException extends Exception {
    public SheetArchivedException(String message) {
        super(message);
    }
}
