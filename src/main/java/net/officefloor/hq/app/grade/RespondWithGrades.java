package net.officefloor.hq.app.grade;

import java.util.Set;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGrades {
    public void respond(@Val Sheet sheet, GradeRepository repository, ObjectResponse<GradesResponse> response) {
        Set<String> retired = repository.retired(sheet.id());
        response.send(new GradesResponse(
                Settings.GRADES.stream().map(g -> new GradeResponse(g, retired.contains(g))).toList()));
    }
}
