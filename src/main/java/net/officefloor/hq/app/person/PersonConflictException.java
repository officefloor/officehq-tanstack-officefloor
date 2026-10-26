package net.officefloor.hq.app.person;

/** An edit named a version of the person that is no longer current: someone else saved it since. */
public class PersonConflictException extends Exception {

    public PersonConflictException(String message) {
        super(message);
    }
}
