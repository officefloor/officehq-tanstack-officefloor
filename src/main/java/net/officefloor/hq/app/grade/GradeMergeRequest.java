package net.officefloor.hq.app.grade;

import jakarta.validation.constraints.Size;

/** The two grades the merge selects submit: the one merged and the one it is merged into. */
public record GradeMergeRequest(@Size(max = 10) String from, @Size(max = 10) String into) {
}
