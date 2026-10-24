package net.officefloor.hq.app.pool;

/** A pool, or a pool member, that cannot be added: a blank or taken name, an unknown pool or person, a person
 *  already in the pool, or a share that is not more than 0.00 and at most 100.00 with at most two decimals. */
public class InvalidPoolException extends Exception {
    public InvalidPoolException(String message) {
        super(message);
    }
}
