package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveGradeAdded {
    public void save(@PathVariable("sheetId") String sheetId, @Val GradeAddInput input, GradeStore grades) {
        grades.add(sheetId.trim(), input.grade());
    }
}
