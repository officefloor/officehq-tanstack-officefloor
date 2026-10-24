package net.officefloor.hq.app.sheet;

/** A change to an archived sheet, which can change nothing; or an archive asked for by anyone but a steward, or of
 *  a sheet already archived. */
public class SheetArchivedException extends Exception {
    public SheetArchivedException(String message) {
        super(message);
    }
}
