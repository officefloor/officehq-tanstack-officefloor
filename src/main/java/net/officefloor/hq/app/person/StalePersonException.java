package net.officefloor.hq.app.person;

/** Someone else saved the person since the editor loaded the version it is saving. */
public class StalePersonException extends Exception {
    public StalePersonException(long id) {
        super("The person was changed by someone else: " + id);
    }
}
