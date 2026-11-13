package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildPerson {
    public void build(@Val PersonRequest request, @Val Sheet sheet, Out<Person> person) {
        person.set(new Person(null, sheet.id(), request.name(), request.grade(), 0, 0, null, null, null, List.of())
                .withTerms(request.termsDays()).withContact(request.contactOrNull()));
    }
}
