package net.officefloor.hq.app.grade;

import java.util.List;

/** The grades on a sheet, in order: A, B and C, then those added. */
public record GradesResponse(List<GradeResponse> grades) {
}
