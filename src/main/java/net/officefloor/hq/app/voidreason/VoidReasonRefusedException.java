package net.officefloor.hq.app.voidreason;

/** A void reason that may not be added: blank, or already on the sheet's list. */
public class VoidReasonRefusedException extends Exception {

    public VoidReasonRefusedException(String message) {
        super(message);
    }
}
