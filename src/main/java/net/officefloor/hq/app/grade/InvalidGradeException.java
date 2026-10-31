package net.officefloor.hq.app.grade;

/** A grade that cannot be retired (unknown, or already retired); nothing is written. */
public class InvalidGradeException extends Exception {
    public InvalidGradeException(String message) {
        super(message);
    }
}
