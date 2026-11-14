package net.officefloor.hq.app.person;

/** Someone else saved the person after the editor loaded it: refused and nothing is written. */
public class PersonConflictException extends Exception {
    public PersonConflictException(long id) {
        super("Person " + id + " was changed by someone else");
    }
}
