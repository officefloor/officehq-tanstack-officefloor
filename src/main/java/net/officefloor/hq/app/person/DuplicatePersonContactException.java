package net.officefloor.hq.app.person;

/** A person add or edit to a contact another person on the sheet already has. */
public class DuplicatePersonContactException extends Exception {
    public DuplicatePersonContactException(String contact) {
        super("The contact " + contact + " is already in use on this sheet.");
    }
}
