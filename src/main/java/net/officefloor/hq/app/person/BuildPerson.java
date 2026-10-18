package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class BuildPerson {
    public void build(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, Out<Person> personOut) {
        Person person = new Person(sheetId.trim());
        person.setName(request.name());
        person.recordGrade(request.grade(), null); // a first grade applies from the beginning of time
        personOut.set(person);
    }
}
