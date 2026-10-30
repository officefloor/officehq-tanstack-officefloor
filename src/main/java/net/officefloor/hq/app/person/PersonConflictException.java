package net.officefloor.hq.app.person;

/** A person saved over a version someone else has saved since the editor loaded it; nothing is written. */
public class PersonConflictException extends Exception {
    public PersonConflictException(long personId) {
        super("Person " + personId + " was changed by someone else; reload and save again");
    }
}
