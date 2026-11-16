package net.officefloor.hq.app.pool;

/** A pool or pool member that cannot be added: a blank or taken pool name, an unknown pool or person, a person already
 *  in the pool, or a share not more than 0.00 and at most 100.00 with two decimals. */
public class InvalidPoolException extends Exception {
    public InvalidPoolException(String message) {
        super(message);
    }
}
