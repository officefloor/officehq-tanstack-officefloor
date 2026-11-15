package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

public class SavePerson {
    public void save(@RequestParam(name = "by", required = false) String by, @Val Person person,
            PersonRepository people) throws PersonConflictException {
        if (!people.update(person)) {
            throw new PersonConflictException(person.getId()); // saved by someone else meanwhile
        }
        if (person.getRecorded() != null) {
            people.saveGrade(person, person.getRecorded(), by == null ? "" : by);
        }
        if (person.getRecordedTerms() != null) {
            people.saveTerms(person, person.getRecordedTerms(), by == null ? "" : by);
        }
    }
}
