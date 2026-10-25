package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeople {
    public void respond(@Val SheetResponse sheet, PersonRepository repository,
            ObjectResponse<List<PersonResponse>> response) {
        response.send(repository.list(sheet.id()).stream().map(PersonResponse::from).toList());
    }
}
