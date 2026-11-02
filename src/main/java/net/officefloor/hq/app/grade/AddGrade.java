package net.officefloor.hq.app.grade;

import java.util.regex.Pattern;
import net.officefloor.hq.app.Audit;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Adds a grade to the sheet: a code of one to three capital letters (trimmed) not yet one of its grades; anything else
 * is refused. The new grade can then be chosen for a person and given rates.
 */
public class AddGrade {
    private static final Pattern CODE = Pattern.compile("[A-Z]{1,3}");

    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody GradeRequest request, Grades grades, Audit audit) throws InvalidGradeException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!CODE.matcher(grade).matches()) {
            throw new InvalidGradeException("A grade is a code of one to three capital letters");
        }
        if (grades.isGrade(sheet, grade)) {
            throw new InvalidGradeException("Grade " + grade + " is already a grade");
        }
        grades.add(sheet, grade);
        audit.record("GRADE_ADDED sheet=" + sheet + " by=" + user + " grade=" + grade);
    }
}
