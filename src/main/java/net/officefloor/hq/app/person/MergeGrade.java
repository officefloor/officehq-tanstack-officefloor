package net.officefloor.hq.app.person;

import java.time.LocalDate;
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

/** Merge a grade into another: a steward only, and two different grades. GRADE_MERGED is written, then every
 *  person whose grade in force on the sheet's today is the merged grade is given the other from the sheet's
 *  today, in list order (by their position), each exactly as the person form would save them with their own
 *  name and terms: each writes PERSON_EDITED and its rollup lines. Then the merge is made retroactive: every
 *  grade record on the merged grade of each person it moved becomes the other, so all their claims, whatever
 *  their date, use the other grade's rates, terms and thresholds (frozen and statement-bound figures stay as
 *  they are). Then the merge as a whole records the rollup lines for whatever moved since before it. Merging
 *  into a retired grade, or on an archived sheet, is refused. */
public class MergeGrade {

    public void merge(@PathVariable("sheetId") String sheetId, @RequestBody GradeMergeRequest request,
            ServerHttpConnection connection, PersonRepository repository, SheetRepository sheets, Audit audit,
            Rollup rollup, SettingsRepository settings, Out<GradeMergeResult> result) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        String from = request == null || request.from() == null ? "" : request.from().trim();
        String into = request == null || request.into() == null ? "" : request.into().trim();
        if (!settings.gradeExists(id, from) || !settings.gradeExists(id, into)) {
            throw new BadRequestException("grades must be grades of the sheet");
        }
        if (from.equals(into)) {
            throw new BadRequestException("a grade cannot be merged into itself");
        }
        if (!settings.role(id, by).equals(SettingsRepository.STEWARD)) {
            throw new BadRequestException("only a steward can merge grades");
        }
        if (settings.archived(id)) {
            throw new BadRequestException("the sheet is archived");
        }
        NewPerson.checkNotRetired(settings, id, into);
        audit.record("GRADE_MERGED sheet=" + id + " by=" + by + " from=" + from + " into=" + into);
        LocalDate today = sheets.today(id);
        Rollup.Snapshot start = rollup.take(id);
        java.util.List<Long> moved = new java.util.ArrayList<>();
        for (Person current : repository.list(id)) {
            if (!from.equals(current.grade())) continue;
            Rollup.Snapshot before = rollup.take(id);
            if (!repository.update(id, current.id(), current.version(), current.name(), current.terms())) continue;
            repository.grade(id, current.id(), into, today);
            Person saved = repository.find(id, current.id());
            audit.record("PERSON_EDITED sheet=" + id + " by=" + by + " name=" + saved.name() + " grade=" + saved.grade());
            rollup.record(id, by, before);
            moved.add(current.id());
        }
        for (long personId : moved) {
            for (GradeFrom g : repository.grades(id, personId)) {
                if (!from.equals(g.grade())) continue;
                repository.grade(id, personId, into, g.from() == null ? null : LocalDate.parse(g.from()));
            }
        }
        rollup.record(id, by, start);
        result.set(new GradeMergeResult(moved.size()));
    }
}
