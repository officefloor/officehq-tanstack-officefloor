package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Retire the grade on the sheet, refusing one already retired. */
public class SaveGradeRetire {
    public void save(@PathVariable("sheetId") String sheetId, @Val GradeRetireRequest request, PeriodRepository periods,
            GradeRepository grades) throws InvalidGradeException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        if (grades.isRetired(id, request.grade())) {
            throw new InvalidGradeException("The grade " + request.grade() + " is already retired.");
        }
        grades.retire(id, request.grade(), request.user());
    }
}
