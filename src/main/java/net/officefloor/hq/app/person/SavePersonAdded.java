package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SavePersonAdded {
    public void save(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, PersonStore store,
            Out<Person> person) {
        person.set(store.add(sheetId.trim(), request.name(), request.grade(),
                request.terms() == null ? null : Integer.valueOf(request.terms())));
    }
}
