package net.officefloor.hq.app.person;

/** No person with this id on the sheet. */
public class PersonNotFoundException extends Exception {
    public PersonNotFoundException(long personId) {
        super("No such person: " + personId);
    }
}
