package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Give the selected people a grade together, in list order (the order they were added), each exactly as
 *  the person form would save them with their own name and terms: the grade is recorded from the from-date
 *  (blank: the sheet's today), except that leaving the grade in force on the sheet's today as it is, with
 *  the from-date blank, records no grade; each writes PERSON_EDITED and its rollup lines. An id that is no
 *  person on the sheet is skipped. A viewer's selection is all refused: no one is graded. An archived sheet
 *  refuses it. */
public class BulkGradePeople {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void grade(@PathVariable("sheetId") String sheetId, @RequestBody BulkGradeRequest request,
            ServerHttpConnection connection, PersonRepository repository, SheetRepository sheets, Audit audit,
            Rollup rollup, SettingsRepository settings, Out<BulkGradeResult> result) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        Set<Long> selected = new LinkedHashSet<>();
        if (request != null && request.personIds() != null) {
            for (Long pid : request.personIds()) {
                if (pid != null) selected.add(pid);
            }
        }
        if (selected.isEmpty()) {
            throw new BadRequestException("select at least one person");
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) {
            throw new BadRequestException("grade must be A, B or C");
        }
        String fromText = request.from() == null ? "" : request.from().trim();
        LocalDate from;
        try {
            from = fromText.isEmpty() ? sheets.today(id) : LocalDate.parse(fromText);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("from-date must be yyyy-mm-dd");
        }
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        int graded = 0;
        if (settings.role(id, by).equals(SettingsRepository.VIEWER)) {
            result.set(new BulkGradeResult(graded));
            return;
        }
        for (Person current : repository.list(id)) {
            if (!selected.contains(current.id())) continue;
            boolean regrade = !fromText.isEmpty() || !grade.equals(current.grade());
            Rollup.Snapshot before = rollup.take(id);
            if (!repository.update(id, current.id(), current.version(), current.name(), current.terms())) continue;
            if (regrade) {
                repository.grade(id, current.id(), grade, from);
            }
            Person saved = repository.find(id, current.id());
            audit.record("PERSON_EDITED sheet=" + id + " by=" + by + " name=" + saved.name() + " grade=" + saved.grade());
            rollup.record(id, by, before);
            graded++;
        }
        result.set(new BulkGradeResult(graded));
    }
}
