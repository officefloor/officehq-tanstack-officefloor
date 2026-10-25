package net.officefloor.hq.app.person;

/** A grade a person was given and its from-date (null: from the beginning of time). */
public record GradeResponse(String grade, String from) {
    public static GradeResponse from(GradeFrom grade) {
        return new GradeResponse(grade.grade(), grade.from() == null ? null : grade.from().toString());
    }
}
