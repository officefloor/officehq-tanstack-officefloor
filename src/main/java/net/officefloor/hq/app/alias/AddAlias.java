package net.officefloor.hq.app.alias;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Adds an alias (trimmed) to the person; refused when blank or equal, ignoring case, to any name or alias on the sheet
 * or on another sheet of its tree (its ancestors and descendants).
 */
public class AddAlias {
    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @RequestBody AliasRequest request, People people, Aliases aliases, Audit audit) throws InvalidAliasException {
        String alias = request == null || request.alias() == null ? "" : request.alias().trim();
        if (alias.isEmpty()) {
            throw new InvalidAliasException("An alias is required");
        }
        if (people.nameTaken(sheet, alias, null) || aliases.taken(sheet, alias)
                || people.usedInTree(sheet, alias)) {
            throw new InvalidAliasException("The name is already in use: " + alias);
        }
        aliases.add(sheet, person.id(), alias);
        audit.record("PERSON_ALIAS_ADDED sheet=" + sheet + " by=" + user + " name=" + person.name() + " alias="
                + alias);
    }
}
