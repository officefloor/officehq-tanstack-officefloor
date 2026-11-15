package net.officefloor.hq.app.setting;

/** A viewer may change nothing on the sheet. Refused and nothing is written. */
public class ViewerRefusedException extends Exception {
    public ViewerRefusedException(String message) {
        super(message);
    }
}
