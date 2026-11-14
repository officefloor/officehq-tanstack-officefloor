package net.officefloor.hq.app.person;

/** No such person on the sheet. */
public class PersonNotFoundException extends Exception {
    public PersonNotFoundException(long id) {
        super("No person " + id);
    }
}
