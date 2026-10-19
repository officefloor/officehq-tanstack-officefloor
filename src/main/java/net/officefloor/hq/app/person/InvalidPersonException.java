package net.officefloor.hq.app.person;

/** The person form was refused: a blank name, a name already in use on the sheet, or a grade other
 *  than A, B or C. */
public class InvalidPersonException extends Exception {
    public InvalidPersonException(String message) {
        super(message);
    }
}
