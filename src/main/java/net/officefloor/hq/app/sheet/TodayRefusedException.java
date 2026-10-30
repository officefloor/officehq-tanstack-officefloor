package net.officefloor.hq.app.sheet;

/** A change of the sheet's today asked for by a role the permission matrix does not allow to change it. */
public class TodayRefusedException extends Exception {

    public TodayRefusedException(String message) {
        super(message);
    }
}
