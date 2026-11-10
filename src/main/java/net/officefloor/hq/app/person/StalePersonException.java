package net.officefloor.hq.app.person;

/** An edit made from a version of the person that someone else has since saved over. */
public class StalePersonException extends Exception {
    public StalePersonException(String id) {
        super("Person " + id + " was changed by someone else");
    }
}
