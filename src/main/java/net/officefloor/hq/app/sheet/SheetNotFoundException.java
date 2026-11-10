package net.officefloor.hq.app.sheet;

/** A sheet id that was never loaded. */
public class SheetNotFoundException extends Exception {
    public SheetNotFoundException(String id) {
        super("No sheet " + id);
    }
}
