package net.officefloor.hq.app.crew;

/** A crew, or a crew member, that cannot be added: a blank or taken name, an unknown crew or person, or a person who
 *  is the crew's lead or already in it. */
public class InvalidCrewException extends Exception {
    public InvalidCrewException(String message) {
        super(message);
    }
}
