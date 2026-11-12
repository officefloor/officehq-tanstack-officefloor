package net.officefloor.hq.app.person;

/** An edit made on a version of the person someone else has since saved over; answered 409. */
public class StalePersonException extends Exception {
    public StalePersonException() {
        super("Someone else has changed this person; the form now shows their values");
    }
}
