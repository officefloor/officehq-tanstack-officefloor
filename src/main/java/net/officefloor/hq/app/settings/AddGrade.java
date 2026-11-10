package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Add a grade to the sheet: a code of one to three capital letters not yet one of its grades. Writes GRADE_ADDED. */
public class AddGrade {
    public void add(@PathVariable("sheetId") String sheetId, @RequestBody GradeAddRequest request,
            ServerHttpConnection connection, SettingsRepository repository, Audit audit, Out<Settings> settings)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!SettingsRepository.GRADE_CODE.matcher(grade).matches()) {
            throw new BadRequestException("a grade is one to three capital letters");
        }
        if (repository.gradeExists(id, grade)) {
            throw new BadRequestException("grade " + grade + " already exists");
        }
        repository.addGrade(id, grade);
        audit.record("GRADE_ADDED sheet=" + id + " by=" + by + " grade=" + grade);
        settings.set(repository.load(id));
    }
}
