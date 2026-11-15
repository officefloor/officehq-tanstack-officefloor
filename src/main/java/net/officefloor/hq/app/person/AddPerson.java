package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class AddPerson {
    public void add(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @Val PersonRequest request, SheetRepository sheets, PersonRepository people,
            Out<Person> person) throws InvalidPersonException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        if (people.nameTaken(id, request.name(), null) || people.nameTakenInTree(id, request.name())) {
            throw new InvalidPersonException("The name is already in use");
        }
        if (request.contact() != null && people.contactTaken(id, request.contact(), null)) {
            throw new InvalidPersonException("The contact is already in use");
        }
        sheets.open(id, user);
        Integer terms = request.terms() == null ? null : Integer.valueOf(request.terms());
        // Terms with a from-date apply from it; with none, from the beginning of time.
        Person added = people.add(id, request.name(), request.grade(),
                request.termsFrom() == null ? terms : null, request.contact(), request.tags(), user);
        if (request.termsFrom() != null) {
            added.giveTermsFrom(terms, request.termsFrom());
            people.saveTerms(added, added.getRecordedTerms(), user);
        }
        person.set(added);
    }
}
