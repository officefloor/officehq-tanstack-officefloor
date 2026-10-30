package net.officefloor.hq.app.pool;

/** A pool or pool member that cannot be added; nothing is written. */
public class InvalidPoolException extends Exception {
    public InvalidPoolException(String message) {
        super(message);
    }
}
