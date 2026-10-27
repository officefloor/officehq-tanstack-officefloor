package net.officefloor.hq.app.person;

/** A merge that may not happen: not a steward, the same person, no such person, or either of them retired. */
public class PersonMergeRefusedException extends Exception {

    public PersonMergeRefusedException(String message) {
        super(message);
    }
}
