package net.officefloor.hq.app.setting;

/** A change asked for by a user the sheet makes a viewer, who may change nothing. */
public class ViewerRefusedException extends Exception {

    public ViewerRefusedException(String message) {
        super(message);
    }
}
