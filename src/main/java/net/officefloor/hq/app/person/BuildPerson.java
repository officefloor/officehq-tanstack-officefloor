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
            throws DuplicatePersonNameException, InvalidPersonException {
        Rollup.Snapshot before = rollup.take(sheet);
        String contact = request.contactOrNull();
        if (contact != null && people.contactTaken(sheet, contact, null)) {
            throw new InvalidPersonException("The contact is already in use: " + contact);
        }
        Person added = people.add(sheet, request.name(), request.grade(), request.termsDays(), contact);
        if (request.tagList() != null && !request.tagList().isEmpty()) {
            people.setTags(added.id(), request.tagList());
            added = people.find(sheet, added.id());
        }
        person.set(added);
        audit.record("PERSON_ADDED sheet=" + sheet + " by=" + user + " name=" + added.name() + " grade="
                + added.grade() + (added.terms() == null ? "" : " terms=" + added.terms())
                + (added.contact() == null ? "" : " contact=" + added.contact()));
        rollup.record(sheet, user, before);
    }
}
