package net.officefloor.hq.app.grade;

/** The grade is not one of A, B or C, or is already retired: refused and nothing is written. */
public class InvalidGradeException extends Exception {
    public InvalidGradeException(String message) {
        super(message);
    }
}
