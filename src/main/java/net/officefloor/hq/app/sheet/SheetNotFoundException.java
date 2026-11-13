package net.officefloor.hq.app.sheet;

public class SheetNotFoundException extends Exception {
    public SheetNotFoundException(String id) {
        super("No sheet " + id);
    }
}
