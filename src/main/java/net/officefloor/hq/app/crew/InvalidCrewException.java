package net.officefloor.hq.app.crew;

/** The crew's name is blank or already used on the sheet, its lead is not a person of the sheet, or the
 *  member is not on the sheet, is the crew's lead or is already in the crew: refused and nothing is written. */
public class InvalidCrewException extends Exception {
    public InvalidCrewException(String message) {
        super(message);
    }
}
