package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Gives the person an alias: never one equal, as names are compared, to any name or alias on the sheet or another sheet of its tree. */
public class AddAlias {
    public void add(@Val AliasRequest request, @Val Person person, People people, Audit audit)
            throws InvalidPersonException {
        if (people.nameTaken(person.getSheetId(), request.alias(), null)) {
            throw new InvalidPersonException("The alias is already in use");
        }
        people.addAlias(person, request.alias());
        audit.recordOnCommit("PERSON_ALIAS_ADDED sheet=" + person.getSheetId() + " by=" + request.user()
                + " name=" + person.getName() + " alias=" + request.alias());
    }
}
