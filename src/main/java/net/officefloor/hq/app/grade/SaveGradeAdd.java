package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Add the grade to the sheet, refusing one already a grade. */
public class SaveGradeAdd {
    public void save(@PathVariable("sheetId") String sheetId, @Val GradeAddRequest request, PeriodRepository periods,
            GradeRepository grades) throws InvalidGradeException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        if (grades.isGrade(id, request.grade())) {
            throw new InvalidGradeException("The grade " + request.grade() + " is already a grade.");
        }
        grades.add(id, request.grade(), request.user());
    }
}
