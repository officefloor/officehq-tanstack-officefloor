package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class BuildPerson {
    public void build(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, Out<Person> personOut) {
        Person person = new Person(sheetId.trim());
        person.setName(request.name());
        person.setGrade(request.grade());
        personOut.set(person);
    }
}
