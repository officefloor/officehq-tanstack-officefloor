package net.officefloor.hq.app.grade;

/** A grade retire with no signed-in user, a grade that is not A, B or C, or a grade already retired. */
public class InvalidGradeException extends Exception {
    public InvalidGradeException(String message) {
        super(message);
    }
}
