package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Names another person on the sheet as the person's patron: never themselves, never the same person twice. */
public class AddPatron {
    public void add(@Val PatronRequest request, @Val Person person, People people, Audit audit)
            throws InvalidPersonException {
        Person patron = null;
        try {
            patron = people.find(person.getSheetId(), Long.parseLong(request.patronId().trim()));
        } catch (NumberFormatException e) {
            // not an id this app assigned
        }
        if (patron == null) {
            throw new InvalidPersonException("The patron is not on the sheet");
        }
        if (patron.getId() == person.getId()) {
            throw new InvalidPersonException("A person cannot be their own patron");
        }
        if (person.getPatronIds().contains(patron.getId())) {
            throw new InvalidPersonException(patron.getName() + " is already a patron");
        }
        people.addPatron(person, patron);
        audit.recordOnCommit("PATRON_ADDED sheet=" + person.getSheetId() + " by=" + request.user()
                + " name=" + person.getName() + " patron=" + patron.getName());
    }
}
