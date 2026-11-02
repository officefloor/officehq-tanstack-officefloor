package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Retires a grade of the sheet; refused for a grade not the sheet's or already retired. People on it keep it, but it
 * can no longer be chosen for a person.
 */
public class RetireGrade {
    public void retire(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody GradeRequest request, Grades grades, Audit audit) throws InvalidGradeException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!grades.isGrade(sheet, grade)) {
            throw new InvalidGradeException("Grade " + grade + " is not one of the sheet's");
        }
        if (grades.isRetired(sheet, grade)) {
            throw new InvalidGradeException("Grade " + grade + " is already retired");
        }
        grades.retire(sheet, grade);
        audit.record("GRADE_RETIRED sheet=" + sheet + " by=" + user + " grade=" + grade);
    }
}
