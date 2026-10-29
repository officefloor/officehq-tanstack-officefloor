package net.officefloor.hq.app.grade;

/** A grade that may not be retired: not A, B or C, or already retired. */
public class GradeRefusedException extends Exception {

    public GradeRefusedException(String message) {
        super(message);
    }
}
