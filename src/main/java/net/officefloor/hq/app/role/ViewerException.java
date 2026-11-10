package net.officefloor.hq.app.role;

/** A change attempted by a viewer, who may change nothing. */
public class ViewerException extends Exception {
    public ViewerException(String message) {
        super(message);
    }
}
