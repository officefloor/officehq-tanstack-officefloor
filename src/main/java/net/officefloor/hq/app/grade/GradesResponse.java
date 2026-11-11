package net.officefloor.hq.app.grade;

import java.util.List;

/** The grades A, B and C on a sheet, in order. */
public record GradesResponse(List<GradeResponse> grades) {
}
