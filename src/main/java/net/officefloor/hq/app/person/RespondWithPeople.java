package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeople {
    public void respond(@Val Sheet sheet, PersonRepository repository, ObjectResponse<List<PersonResponse>> response) {
        response.send(repository.findBySheetIdOrderByIdAsc(sheet.getId()).stream().map(PersonResponse::from).toList());
    }
}
