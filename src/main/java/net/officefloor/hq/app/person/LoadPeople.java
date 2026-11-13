package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadPeople {
    public void load(@Val Sheet sheet, PersonRepository repository, Out<List<Person>> people) {
        people.set(repository.findAll(sheet.id()));
    }
}
