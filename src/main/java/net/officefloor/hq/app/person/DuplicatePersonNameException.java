package net.officefloor.hq.app.person;

/** A name already in use by another person on the sheet (ignoring case and surrounding spaces). */
public class DuplicatePersonNameException extends Exception {
    public DuplicatePersonNameException(String name) {
        super("The name " + name + " is already in use");
    }
}
