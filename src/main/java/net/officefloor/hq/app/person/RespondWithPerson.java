package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPerson {
    public void respond(@Val Sheet sheet, @Val Person person, PersonRepository people, SheetRepository sheets,
            ObjectResponse<PersonResponse> response) {
        boolean clashing = PersonRepository.clashes(person, people.namesIn(sheets.tree(sheet.id())));
        response.send(PersonResponse.from(person, sheet.today(), clashing));
    }
}
