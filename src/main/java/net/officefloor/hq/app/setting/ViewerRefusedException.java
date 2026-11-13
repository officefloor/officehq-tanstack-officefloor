package net.officefloor.hq.app.setting;

/** A change asked for by a viewer, who can change nothing. */
public class ViewerRefusedException extends Exception {
    public ViewerRefusedException(String message) {
        super(message);
    }
}
