package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the person, their own terms from the beginning of time, or from the terms from-date when given. */
public class SavePersonAdded {
    public void save(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, PersonStore store,
            Out<Person> person) {
        boolean dated = request.termsFrom() != null;
        Person added = store.add(sheetId.trim(), request.name(), request.grade(),
                request.terms() == null || dated ? null : Integer.valueOf(request.terms()), request.contact(), request.tags());
        if (dated) {
            PersonRules.applyTerms(added, request.terms(), request.termsFrom());
            store.recordTerms(added, added.getTermsRecordFrom());
        }
        person.set(added);
    }
}
