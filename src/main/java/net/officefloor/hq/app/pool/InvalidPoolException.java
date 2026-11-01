package net.officefloor.hq.app.pool;

/** The pool's name is blank or taken, or the member is already in the pool, unknown, or has a bad share. */
public class InvalidPoolException extends Exception {
    public InvalidPoolException(String message) {
        super(message);
    }
}
