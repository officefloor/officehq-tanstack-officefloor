package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Figures;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

public class AddImportedPeople {
    // Each checked line is stored in order. A new person is added as the person form adds one: PERSON_ADDED, then its
    // ROLLUP lines. A matched person is updated as their edit form saves them: they keep their name, their grade is
    // given from the sheet's today unless it is already the one in force then, and their terms and contact are set when
    // the line gives them; PERSON_EDITED, then the ROLLUP lines.
    public void add(@PathVariable("sheetId") String sheetId, @Val ImportedPeople imported, @Val Figures before,
            People people, Sheets sheets, Rollup rollup, ServerHttpConnection connection, Audit audit,
            Out<ImportResponse> result) {
        String sheet = sheetId.trim();
        String user = Audit.user(connection);
        LocalDate today = sheets.today(sheet);
        Figures figures = before;
        int added = 0;
        int updated = 0;
        for (ImportedPeople.Line line : imported.people()) {
            PersonRequest request = line.request();
            Person person = line.existing();
            if (person == null) {
                new AuditPersonAdded().audit(people.add(sheet, request), connection, audit);
                added++;
            } else {
                Person edited = person.with(new PersonRequest(person.name(), request.grade(), null, null,
                        request.terms() != null ? request.terms() : person.terms() == null ? null : person.terms().toString(),
                        request.contact() != null ? request.contact() : person.contact(),
                        person.tags().isEmpty() ? null : String.join(",", person.tags()), null));
                people.save(edited);
                if (!request.grade().equals(person.gradeOn(today))) {
                    people.gradeFrom(person.id(), today, request.grade());
                }
                Person stored = people.find(sheet, person.id());
                new AuditPersonEdited().audit(person, PersonResponse.from(stored, today), connection, audit);
                updated++;
            }
            figures = RollupLines.record(sheet, user, figures, rollup, audit);
        }
        result.set(new ImportResponse(added, updated));
    }
}
