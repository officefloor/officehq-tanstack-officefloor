package net.officefloor.hq.app.patron;

/** A patron that may not be named: not on the sheet, the person themselves, or already named. */
public class PatronRefusedException extends Exception {

    public PatronRefusedException(String message) {
        super(message);
    }
}
