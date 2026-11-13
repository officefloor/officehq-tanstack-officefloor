package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds the person when they are new to the sheet, recording it as the person form's add does. */
public class SaveQuickPerson {
    public void save(@Val Person person, PersonRepository repository, @RequestParam("user") String user,
            Audit audit, Out<Person> saved) {
        if (person.id() != null) {
            return;
        }
        Person stored = repository.save(person);
        audit.record("PERSON_ADDED sheet=" + stored.sheetId() + " by=" + user + " name=" + stored.name()
                + " grade=" + stored.grade());
        saved.set(stored);
    }
}
