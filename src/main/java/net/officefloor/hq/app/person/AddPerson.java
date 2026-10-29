package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class AddPerson {
    public void add(@Val PersonRequest request, @PathVariable("sheetId") String sheetId, People people,
            net.officefloor.hq.app.grade.Grades grades, Out<Person> personOut) throws InvalidPersonException {
        if (grades.retired(sheetId.trim(), request.grade())) {
            throw new InvalidPersonException("Grade " + request.grade() + " is retired.");
        }
        if (people.nameTaken(sheetId.trim(), request.name(), -1)) {
            throw new InvalidPersonException("The name is already in use.");
        }
        if (request.contact() != null && people.contactTaken(sheetId.trim(), request.contact(), -1)) {
            throw new InvalidPersonException("The contact is already in use.");
        }
        personOut.set(people.add(sheetId.trim(), request));
    }
}
