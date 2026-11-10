package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Retire a grade of the sheet; one already retired is refused. Writes GRADE_RETIRED. */
public class RetireGrade {
    public void retire(@PathVariable("sheetId") String sheetId, @RequestBody GradeRetireRequest request,
            ServerHttpConnection connection, SettingsRepository repository, Audit audit, Out<Settings> settings)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        repository.requireGrade(id, grade);
        if (repository.retired(id, grade)) {
            throw new BadRequestException("grade " + grade + " is already retired");
        }
        repository.retireGrade(id, grade);
        audit.record("GRADE_RETIRED sheet=" + id + " by=" + by + " grade=" + grade);
        settings.set(repository.load(id));
    }
}
