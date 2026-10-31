package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person) {
        person.setName(request.name());
        person.setGrade(request.grade());
    }
}
