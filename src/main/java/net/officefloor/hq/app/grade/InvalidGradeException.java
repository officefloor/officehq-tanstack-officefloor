package net.officefloor.hq.app.grade;

/** The grade is not one of the sheet's, or is already retired. */
public class InvalidGradeException extends Exception {
    public InvalidGradeException(String message) {
        super(message);
    }
}
