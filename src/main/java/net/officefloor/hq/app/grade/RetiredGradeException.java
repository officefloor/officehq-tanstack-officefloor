package net.officefloor.hq.app.grade;

/** A retired grade chosen for a person. */
public class RetiredGradeException extends Exception {
    public RetiredGradeException(String grade) {
        super("The grade " + grade + " is retired: choose another.");
    }
}
