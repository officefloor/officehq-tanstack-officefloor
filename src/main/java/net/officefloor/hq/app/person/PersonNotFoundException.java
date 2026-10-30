package net.officefloor.hq.app.person;

public class PersonNotFoundException extends Exception {
    public PersonNotFoundException(String personId) {
        super("No such person: " + personId);
    }
}
