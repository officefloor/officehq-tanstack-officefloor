package net.officefloor.hq.app.grade;

/** A grade that cannot be retired: not one of the sheet's grades, or already retired; or one to add that is not one to three capital letters, or already a grade. */
public class InvalidGradeException extends Exception {
    public InvalidGradeException(String message) {
        super(message);
    }
}
