package net.officefloor.hq.app.person;

public class PersonNotFoundException extends Exception {
    public PersonNotFoundException(long id) {
        super("No person " + id);
    }
}
