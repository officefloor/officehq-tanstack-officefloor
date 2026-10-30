package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGradeMerge {

    // Only a steward (a delegation included) merges a grade; both grades, trimmed, must be the sheet's, and differ. The
    // grade merged into must not be retired, as the person form refuses giving one.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody MergeRequest body, Grades grades,
            Settings settings, ServerHttpConnection connection, Out<MergedGrade> out) throws GradeRefusedException {
        String sheet = sheetId.trim();
        if (!Settings.STEWARD.equals(settings.role(sheet, Audit.user(connection).trim()))) {
            throw new GradeRefusedException("Only a steward can merge a grade.");
        }
        String from = body == null || body.from() == null ? "" : body.from().trim();
        String into = body == null || body.into() == null ? "" : body.into().trim();
        if (!grades.exists(sheet, from) || !grades.exists(sheet, into)) {
            throw new GradeRefusedException("The grades must be the sheet's grades.");
        }
        if (from.equals(into)) {
            throw new GradeRefusedException("A grade cannot be merged into itself.");
        }
        if (grades.retired(sheet, into)) {
            throw new GradeRefusedException("Grade " + into + " is retired.");
        }
        out.set(new MergedGrade(sheet, from, into));
    }
}
