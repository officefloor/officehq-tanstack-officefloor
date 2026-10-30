package net.officefloor.hq.app.setting;

/** A change asked for by a viewer: a viewer sees the sheet and changes nothing, so it is refused and nothing is written. */
public class ViewerRefusedException extends Exception {
    public ViewerRefusedException() {
        this("A viewer can change nothing on the sheet");
    }

    public ViewerRefusedException(String message) {
        super(message);
    }
}
