package net.officefloor.hq.app.crew;

/** The crew's name is blank or taken, its lead unknown, or the member unknown, its lead or already in it. */
public class InvalidCrewException extends Exception {
    public InvalidCrewException(String message) {
        super(message);
    }
}
