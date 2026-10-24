package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.statement.StatementRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPerson {
    public void respond(@Val Person person, StatementRepository statements, PatronRepository patrons, PersonAliasRepository aliases,
            PersonGradeRepository grades, SheetRepository sheets, TreeNames tree,
            ObjectResponse<PersonResponse> response) {
        Sheet sheet = sheets.findById(person.getSheetId()).orElseGet(() -> new Sheet(person.getSheetId()));
        response.send(PersonResponse.from(person, statements.findByPersonIdOrderByIdAsc(person.getId()),
                patrons.findByPersonIdOrderByIdAsc(person.getId()),
                aliases.findByPersonIdOrderByIdAsc(person.getId()), grades.gradesOf(person), sheet.getToday(),
                tree.usedAround(sheet.getId())));
    }
}
