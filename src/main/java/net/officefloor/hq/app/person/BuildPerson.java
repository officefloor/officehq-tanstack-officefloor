package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class BuildPerson {
    public void build(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val PersonRequest request, People people, Audit audit, Rollup rollup, Out<Person> person)
            throws DuplicatePersonNameException {
        Rollup.Snapshot before = rollup.take(sheet);
        Person added = people.add(sheet, request.name(), request.grade(), request.termsDays());
        person.set(added);
        audit.record("PERSON_ADDED sheet=" + sheet + " by=" + user + " name=" + added.name() + " grade="
                + added.grade() + (added.terms() == null ? "" : " terms=" + added.terms()));
        rollup.record(sheet, user, before);
    }
}
