package net.officefloor.hq.app.sheet;

/** A sheet id that has never been loaded. */
public class SheetNotFoundException extends Exception {
    public SheetNotFoundException(String id) {
        super("No sheet " + id + ".");
    }
}
