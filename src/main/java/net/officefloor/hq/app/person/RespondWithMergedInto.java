package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.StatementRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the person merged into, as they now stand. */
public class RespondWithMergedInto {
    public void respond(@Val PersonMerged merged, StatementRepository statements, PatronRepository patrons,
            PersonAliasRepository aliases, PersonGradeRepository grades, SheetRepository sheets,
            ObjectResponse<PersonResponse> response) {
        Person into = merged.into();
        Sheet sheet = sheets.findById(into.getSheetId()).orElseGet(() -> new Sheet(into.getSheetId()));
        response.send(PersonResponse.from(into, statements.findByPersonIdOrderByIdAsc(into.getId()),
                patrons.findByPersonIdOrderByIdAsc(into.getId()),
                aliases.findByPersonIdOrderByIdAsc(into.getId()), grades.gradesOf(into), sheet.getToday()));
    }
}
