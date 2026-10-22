package net.officefloor.hq.app.person;

/** A person add or edit to a name another person on the sheet already has. */
public class DuplicatePersonNameException extends Exception {
    public DuplicatePersonNameException(String name) {
        super("The name " + name + " is already in use on this sheet.");
    }
}
