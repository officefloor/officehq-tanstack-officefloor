package net.officefloor.hq.app.pool;

/** A pool or pool member add refused: no signed-in user, a blank or taken name, an unknown pool or person, a person
 *  already in the pool, or a bad share. */
public class InvalidPoolException extends Exception {
    public InvalidPoolException(String message) {
        super(message);
    }
}
