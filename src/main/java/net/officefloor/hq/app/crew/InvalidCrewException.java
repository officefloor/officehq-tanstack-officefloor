package net.officefloor.hq.app.crew;

/** A crew or crew member that cannot be added: a blank or taken crew name, an unknown crew or person, the crew's lead,
 *  or a person already in the crew. */
public class InvalidCrewException extends Exception {
    public InvalidCrewException(String message) {
        super(message);
    }
}
