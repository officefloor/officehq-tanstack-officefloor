package net.officefloor.hq.app.person;

/** An edit of a person someone else has saved since the editor loaded them. */
public class PersonConflictException extends Exception {
    public PersonConflictException() {
        super("Someone else has saved this person since you opened the editor.");
    }
}
