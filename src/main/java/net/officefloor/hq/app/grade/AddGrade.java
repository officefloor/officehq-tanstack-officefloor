package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class AddGrade {
    public void add(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val GradeRequest request, SheetRepository sheets, GradeRepository grades,
            Out<GradeAdded> added) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        sheets.open(id, user);
        added.set(grades.add(id, request.grade(), user));
    }
}
