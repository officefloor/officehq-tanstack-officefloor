package net.officefloor.hq.app.crew;

/** A crew or crew member that may not be added: a blank or taken name, a lead not of the sheet, or a member who is the
 * crew's lead or already in it. */
public class CrewRefusedException extends Exception {

    public CrewRefusedException(String message) {
        super(message);
    }
}
