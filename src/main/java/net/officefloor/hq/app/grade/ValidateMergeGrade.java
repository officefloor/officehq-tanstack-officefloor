package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refused for a grade not A, B or C, and for merging a grade into itself. */
public class ValidateMergeGrade {
    public void validate(@RequestBody MergeGradeRequest request, Out<MergeGradeRequest> body)
            throws InvalidGradeException {
        String from = request == null || request.from() == null ? "" : request.from().trim();
        String into = request == null || request.into() == null ? "" : request.into().trim();
        if (!GradeRepository.GRADES.contains(from) || !GradeRepository.GRADES.contains(into)) {
            throw new InvalidGradeException("Grade must be A, B or C");
        }
        if (from.equals(into)) {
            throw new InvalidGradeException("A grade cannot be merged into itself");
        }
        body.set(new MergeGradeRequest(from, into));
    }
}
