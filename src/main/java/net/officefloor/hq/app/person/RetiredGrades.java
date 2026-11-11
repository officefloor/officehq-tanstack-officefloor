package net.officefloor.hq.app.person;

/** The refusal of a retired grade chosen for a new person or in an edit. */
final class RetiredGrades {
    private RetiredGrades() {
    }

    static InvalidPersonException refused(String grade) {
        return new InvalidPersonException("Grade " + grade + " is retired");
    }

    /** The refusal of a grade that is not one on the sheet. */
    static InvalidPersonException unknown(String grade) {
        return new InvalidPersonException("Grade " + grade + " is not a grade on the sheet");
    }
}
