package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Saves the row's chosen grade exactly as the person form would with its from-date blank (the sheet's today), keeping
 * everything else as stored: the same refusals, and the same PERSON_EDITED and rollup lines.
 */
public class GradePerson {
    public void grade(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @Val InlineGradeRequest request, People people, SheetClock clock, Audit audit, Rollup rollup)
            throws DuplicatePersonNameException, StalePersonException, InvalidPersonException {
        if (people.usedInTree(sheet, person.name()) || people.aliasClashes(sheet, person.id())) {
            throw new DuplicatePersonNameException(person.name());
        }
        SavePerson.apply(sheet, user, person,
                new PersonRequest(person.name(), request.grade(), null, null,
                        person.terms() == null ? "" : person.terms().toString(),
                        person.contact() == null ? "" : person.contact(), null),
                people, clock, audit, rollup);
    }
}
