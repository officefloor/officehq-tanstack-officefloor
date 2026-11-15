package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Gives the person the alias: refused (nothing written) when it equals, as names are compared,
 *  any name or alias on the sheet or on another sheet of its tree. */
public class AddAlias {
    public void add(@RequestParam(name = "by", required = false) String by,
            @Val AliasRequest request, @Val Person person, PersonRepository people,
            Out<Alias> added) throws InvalidPersonException {
        if (people.nameTaken(person.getSheetId(), request.alias(), null)
                || people.nameTakenInTree(person.getSheetId(), request.alias())) {
            throw new InvalidPersonException("The alias is already in use");
        }
        people.addAlias(person, request.alias(), by == null ? "" : by);
        added.set(new Alias(request.alias()));
    }
}
