package net.officefloor.hq.app.crew;

/** A crew or crew member add refused: no signed-in user, a blank or taken name, an unknown crew or person, or a
 *  person who is the crew's lead or already in it. */
public class InvalidCrewException extends Exception {
    public InvalidCrewException(String message) {
        super(message);
    }
}
