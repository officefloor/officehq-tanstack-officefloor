package net.officefloor.hq.app.person;

/** A name already used by another person on the sheet (case and outer spaces ignored). */
public class DuplicatePersonNameException extends Exception {
    public DuplicatePersonNameException(String name) {
        super("The name " + name + " is already in use");
    }
}
