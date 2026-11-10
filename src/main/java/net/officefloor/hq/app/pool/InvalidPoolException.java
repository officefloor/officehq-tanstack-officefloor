package net.officefloor.hq.app.pool;

/**
 * A pool with a blank or taken name, or a member already in the pool, not on the sheet, or with a bad
 * share; or a member pool that is already in a pool, would contain itself, or would make a chain of
 * pools more than three deep.
 */
public class InvalidPoolException extends Exception {
    public InvalidPoolException(String message) {
        super(message);
    }
}
