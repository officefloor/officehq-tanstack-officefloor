package net.officefloor.hq.app.grade;

/** A grade as the front end reads it: its code and whether it is retired. */
public record GradeResponse(String grade, boolean retired) {
}
