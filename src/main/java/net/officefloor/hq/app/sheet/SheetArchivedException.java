package net.officefloor.hq.app.sheet;

/** An archived sheet takes no change. Refused and nothing is written. */
public class SheetArchivedException extends Exception {
    public SheetArchivedException(String message) {
        super(message);
    }
}
