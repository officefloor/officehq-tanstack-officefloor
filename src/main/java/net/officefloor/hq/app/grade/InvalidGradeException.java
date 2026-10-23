package net.officefloor.hq.app.grade;

/** A grade add, retire or merge with no signed-in user, a grade not of the sheet (or, added, not one to three capital
 *  letters or already a grade), or a grade already retired. */
public class InvalidGradeException extends Exception {
    public InvalidGradeException(String message) {
        super(message);
    }
}
