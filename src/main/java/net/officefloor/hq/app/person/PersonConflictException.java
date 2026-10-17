package net.officefloor.hq.app.person;

/** The edit was made against an older version of the person; nothing is changed. */
public class PersonConflictException extends Exception {
    public PersonConflictException(String message) {
        super(message);
    }
}
