package net.officefloor.hq.app.person;

/** The person was saved by someone else since the edit form loaded it. */
public class PersonConflictException extends Exception {
    public PersonConflictException() {
        super("Someone else has saved this person since you opened it. The form now shows the current values.");
    }
}
