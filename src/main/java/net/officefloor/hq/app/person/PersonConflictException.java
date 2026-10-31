package net.officefloor.hq.app.person;

/** An edit made against a version of the person someone else has saved over since; answered 409
 *  with the person as stored now, so the form can show the current values. */
public class PersonConflictException extends Exception {
    private final PersonResponse current;

    public PersonConflictException(PersonResponse current) {
        super("This person was saved by someone else since the editor loaded it");
        this.current = current;
    }

    public PersonResponse getCurrent() { return current; }
}
