package net.officefloor.hq.app.pool;

/** A pool or pool member that may not be added: a blank or taken name, a person already in the pool, a pool
 * already in a pool, a chain of pools that loops or is more than three deep, or a bad share. */
public class PoolRefusedException extends Exception {

    public PoolRefusedException(String message) {
        super(message);
    }
}
