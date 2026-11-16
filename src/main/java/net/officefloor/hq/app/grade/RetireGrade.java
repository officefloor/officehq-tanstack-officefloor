package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RetireGrade {
    public void retire(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val GradeRequest request, SheetRepository sheets, GradeRepository grades,
            Out<GradeRetired> retired) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        sheets.open(id, user);
        retired.set(grades.retire(id, request.grade(), user));
    }
}
