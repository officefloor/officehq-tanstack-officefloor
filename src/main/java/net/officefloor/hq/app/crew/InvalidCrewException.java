package net.officefloor.hq.app.crew;

/** A crew with a blank or taken name or a lead not on the sheet, or a member that is its lead or already in it. */
public class InvalidCrewException extends Exception {
    public InvalidCrewException(String message) {
        super(message);
    }
}
