package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class AddPerson {
    public void add(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val PersonRequest request, SheetRepository sheets, PersonRepository people,
            Out<Person> person) {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        sheets.open(id, user);
        person.set(people.add(id, request.name(), request.grade(), user));
    }
}
