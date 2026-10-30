package net.officefloor.hq.app.crew;

/** A crew or crew member that cannot be added; nothing is written. */
public class InvalidCrewException extends Exception {
    public InvalidCrewException(String message) {
        super(message);
    }
}
