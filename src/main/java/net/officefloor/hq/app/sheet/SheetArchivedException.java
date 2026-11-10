package net.officefloor.hq.app.sheet;

/** A change attempted on an archived sheet, on which nothing may change. */
public class SheetArchivedException extends Exception {
    public SheetArchivedException(String id) {
        super("Sheet " + id + " is archived");
    }
}
