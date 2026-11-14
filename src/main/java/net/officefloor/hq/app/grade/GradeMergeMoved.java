package net.officefloor.hq.app.grade;

import java.util.List;

/** The people a grade merge gave the other grade, by id. */
public record GradeMergeMoved(List<Long> people) {
}
