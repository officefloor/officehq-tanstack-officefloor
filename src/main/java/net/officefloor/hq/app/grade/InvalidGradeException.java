package net.officefloor.hq.app.grade;

/** A grade that cannot be added, retired or merged; nothing is written. */
public class InvalidGradeException extends Exception {
    public InvalidGradeException(String message) {
        super(message);
    }
}
