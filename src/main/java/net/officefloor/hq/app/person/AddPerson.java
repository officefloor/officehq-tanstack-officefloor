package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class AddPerson {
    public void add(@Val PersonRequest request, @PathVariable("sheetId") String sheetId, People people,
            Out<Person> personOut) {
        personOut.set(people.add(sheetId.trim(), request));
    }
}
