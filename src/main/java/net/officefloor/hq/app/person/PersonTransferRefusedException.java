package net.officefloor.hq.app.person;

/**
 * A transfer that may not happen: not a steward, a retired person, no other sheet given (or this sheet), an archived
 * sheet to go to, or a name, alias or contact of theirs already on it.
 */
public class PersonTransferRefusedException extends Exception {

    public PersonTransferRefusedException(String message) {
        super(message);
    }
}
