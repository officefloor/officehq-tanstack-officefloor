package net.officefloor.hq.app.grade;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.ApplyPerson;
import net.officefloor.hq.app.person.AuditPersonEdited;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonConflictException;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.person.PersonResponse;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.rollup.Figures;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class MergeGrade {
    // GRADE_MERGED, then every person whose grade in force on the sheet's today is the merged grade, in list order, is
    // given the other from the sheet's today, each exactly as the person form saves it: the same edit, its PERSON_EDITED
    // line and its ROLLUP lines. A person the edit refuses refuses the whole merge: nothing is stored or recorded.
    public void merge(@Val MergedGrade merge, @Val Figures before, People people, Sheets sheets, Grades grades,
            Rollup rollup, ServerHttpConnection connection, Audit audit, Out<MergeResponse> out) throws GradeRefusedException {
        Audit.Buffer held = new Audit.Buffer(audit);
        String user = Audit.user(connection);
        held.record(Audit.line("GRADE_MERGED", merge.sheetId(), user, "from=" + merge.from(), "into=" + merge.into()));
        LocalDate today = sheets.today(merge.sheetId());
        Figures figures = before;
        int merged = 0;
        for (Person person : people.list(merge.sheetId())) {
            if (!merge.from().equals(person.gradeOn(today))) {
                continue;
            }
            Integer terms = person.ownTermsOn(today);
            PersonRequest request = new PersonRequest(person.name(), merge.into(), person.version(), null,
                    terms == null ? null : terms.toString(), person.contact(),
                    person.tags().isEmpty() ? null : String.join(",", person.tags()), null);
            PersonResponse[] edited = new PersonResponse[1];
            try {
                new ApplyPerson().apply(request, person, people, sheets, grades, r -> edited[0] = r);
            } catch (InvalidPersonException | PersonConflictException e) {
                throw new GradeRefusedException(person.name() + ": " + e.getMessage());
            }
            new AuditPersonEdited().audit(person, edited[0], connection, held);
            figures = RollupLines.record(merge.sheetId(), user, figures, rollup, held);
            merged++;
        }
        held.flush();
        out.set(new MergeResponse(merge.from(), merge.into(), merged));
    }
}
