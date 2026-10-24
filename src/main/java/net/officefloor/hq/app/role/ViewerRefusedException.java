package net.officefloor.hq.app.role;

/** A change by a viewer, who can change nothing on the sheet. */
public class ViewerRefusedException extends Exception {
    public ViewerRefusedException(String message) {
        super(message);
    }
}
