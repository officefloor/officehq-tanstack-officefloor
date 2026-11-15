package net.officefloor.hq.app.pool;

/** The pool's name is blank or already used on the sheet, or the member is already in the pool, not on
 *  the sheet, or has a share that is not more than 0.00 and at most 100.00 with two decimals: refused
 *  and nothing is written. */
public class InvalidPoolException extends Exception {
    public InvalidPoolException(String message) {
        super(message);
    }
}
