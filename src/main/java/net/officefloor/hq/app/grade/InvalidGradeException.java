package net.officefloor.hq.app.grade;

/** A grade that cannot be retired: not one of A, B or C, or already retired. */
public class InvalidGradeException extends Exception {
    public InvalidGradeException(String message) {
        super(message);
    }
}
