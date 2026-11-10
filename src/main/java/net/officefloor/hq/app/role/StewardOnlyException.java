package net.officefloor.hq.app.role;

/** A change only a steward may make, attempted by someone else. */
public class StewardOnlyException extends Exception {
    public StewardOnlyException(String message) {
        super(message);
    }
}
