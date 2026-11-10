package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeople {
    public void respond(@Val Sheet sheet, PersonRepository repository,
            ObjectResponse<PeopleResponse> response) {
        response.send(new PeopleResponse(
                repository.findAll(sheet.id()).stream().map((p) -> PersonResponse.from(p, sheet.today())).toList()));
    }
}
