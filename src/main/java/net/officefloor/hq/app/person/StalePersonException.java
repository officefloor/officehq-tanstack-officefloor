package net.officefloor.hq.app.person;

/** An edit made from a version of the person that someone else has saved over since. */
public class StalePersonException extends Exception {
    public StalePersonException(String name) {
        super(name + " was changed by someone else; review the current values and save again.");
    }
}
