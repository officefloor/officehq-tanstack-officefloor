package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveGradeRetired {
    public void save(@PathVariable("sheetId") String sheetId, @Val GradeRetireInput input, GradeStore grades) {
        grades.retire(sheetId.trim(), input.grade());
    }
}
