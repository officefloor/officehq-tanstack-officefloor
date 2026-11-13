package net.officefloor.hq.app.sheet;

/** A clone of the sheet that is refused: by anyone but a steward, or to a sheet already used. */
public class SheetCloneRefusedException extends Exception {
    public SheetCloneRefusedException(String message) {
        super(message);
    }
}
