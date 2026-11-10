package net.officefloor.hq.app.person;

/** A person id that is not on the sheet. */
public class PersonNotFoundException extends Exception {
    public PersonNotFoundException(String id) {
        super("No person " + id);
    }
}
