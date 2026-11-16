package net.officefloor.hq.app.person;

/** An edit made from a version of the person that someone has saved over since. */
public class StalePersonException extends Exception {
    public StalePersonException(long id) {
        super("Person " + id + " has been changed by someone else");
    }
}
