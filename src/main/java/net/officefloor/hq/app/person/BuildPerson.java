package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildPerson {
    public void build(@Val PersonRequest request, @Val Sheet sheet, Out<Person> personOut) {
        Person person = new Person(null, sheet.id(), request.name(), request.grade());
        person.setTerms(request.termsDays());
        personOut.set(person);
    }
}
