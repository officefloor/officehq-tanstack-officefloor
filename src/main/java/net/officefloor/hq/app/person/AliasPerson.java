package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class AliasPerson {
    // An alias shares the names' uniqueness: it is refused when it equals (ignoring case) any name or alias on the
    // sheet, the person's own included. Person is a record, so the aliased person is published as the response.
    public void alias(@Val Alias alias, @Val Person person, People people, Sheets sheets,
            Out<PersonResponse> responseOut) throws InvalidPersonException {
        if (people.nameOrAliasTaken(person.sheetId(), alias.alias())) {
            throw new InvalidPersonException("The alias is already in use as a name or an alias.");
        }
        people.alias(person.sheetId(), person.id(), alias.alias());
        List<String> aliases = new ArrayList<>(person.aliases());
        aliases.add(alias.alias());
        aliases.sort(String.CASE_INSENSITIVE_ORDER);
        Person aliased = new Person(person.id(), person.sheetId(), person.name(), person.grade(), person.version(),
                person.retiredFrom(), person.holds(), person.dated(), person.terms(), person.contact(), List.copyOf(aliases));
        responseOut.set(PersonResponse.from(aliased, sheets.today(person.sheetId())));
    }
}
