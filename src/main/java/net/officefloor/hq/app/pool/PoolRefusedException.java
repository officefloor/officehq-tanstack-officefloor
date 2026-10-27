package net.officefloor.hq.app.pool;

/** A pool or pool member that may not be added: a blank or taken name, a person already in the pool, or a bad share. */
public class PoolRefusedException extends Exception {

    public PoolRefusedException(String message) {
        super(message);
    }
}
