package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: both grades are A, B or C, and not the same grade. */
public class ValidateMergeGrade {
    public void validate(@RequestBody MergeGradeRequest request, Out<MergeGradeValues> values) throws InvalidGradeException {
        String from = request == null || request.from() == null ? "" : request.from().trim();
        String into = request == null || request.into() == null ? "" : request.into().trim();
        if (!GradeRepository.GRADES.contains(from) || !GradeRepository.GRADES.contains(into)) {
            throw new InvalidGradeException("Grade must be A, B or C");
        }
        if (from.equals(into)) {
            throw new InvalidGradeException("A grade cannot be merged into itself");
        }
        values.set(new MergeGradeValues(from, into, request.by() == null ? "" : request.by().trim()));
    }
}
