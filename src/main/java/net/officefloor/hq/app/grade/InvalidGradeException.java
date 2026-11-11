package net.officefloor.hq.app.grade;

/** A grade other than A, B or C, or one already retired. */
public class InvalidGradeException extends Exception {
    public InvalidGradeException(String message) {
        super(message);
    }
}
