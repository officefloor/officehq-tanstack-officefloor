package net.officefloor.hq.app.person;

/** A person edit made from a version of the person that someone else has saved over since. */
public class StalePersonException extends Exception {
    public StalePersonException(String name) {
        super(name + " was changed by someone else since you opened the editor. The form now shows the current values.");
    }
}
