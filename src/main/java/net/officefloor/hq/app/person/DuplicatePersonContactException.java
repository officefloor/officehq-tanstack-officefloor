package net.officefloor.hq.app.person;

/** A contact already held by another person on the sheet. */
public class DuplicatePersonContactException extends Exception {
    public DuplicatePersonContactException(String contact) {
        super("The contact " + contact + " is already in use");
    }
}
