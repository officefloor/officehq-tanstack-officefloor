package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class BuildPerson {
    public void build(@PathVariable("sheetId") String sheetId, @Val PersonRequest request,
            People people, Out<Person> person) throws InvalidSheetRequestException {
        if (people.nameTaken(sheetId, request.name(), null)) {
            throw new InvalidSheetRequestException("The name is already in use on this sheet");
        }
        person.set(new Person(null, sheetId, request.name(), request.grade()));
    }
}
