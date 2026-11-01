package net.officefloor.hq.app.person;

/** Another person on the sheet already has this name (compared without regard to case). */
public class DuplicatePersonNameException extends Exception {
    public DuplicatePersonNameException(String name) {
        super("The name is already in use: " + name);
    }
}
