package net.officefloor.hq.app.person;

import java.util.stream.Stream;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Val;

/** Gives the person the alias; refuses one equal, as names are compared, to any name or alias on the sheet. */
public class AddAlias {
    public void add(@Val AliasInput input, @Val Person person, PersonRepository repository) throws BadRequestException {
        if (repository.nameTaken(person.getSheetId(), input.alias(), null)
                || !repository.addAlias(person, input.alias())) {
            throw new BadRequestException(PersonRepository.NAME_TAKEN);
        }
        person.setAliases(PersonName.sorted(Stream.concat(person.getAliases().stream(), Stream.of(input.alias())).toList()));
    }
}
