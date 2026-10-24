package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Adds the alias to the person: refused when it equals, ignoring case, any name or alias on the sheet
 *  or on another sheet of its tree. */
public class ApplyAlias {
    public void apply(@Val AliasInput input, @Val Person person, PersonRepository people,
            PersonAliasRepository aliases, TreeNames tree) throws InvalidPersonException {
        if (people.existsBySheetIdAndNameIgnoreCase(person.getSheetId(), input.alias())
                || aliases.existsBySheetIdAndAliasIgnoreCase(person.getSheetId(), input.alias())) {
            throw new InvalidPersonException("The alias is already in use on this sheet.");
        }
        if (tree.usedAround(person.getSheetId(), input.alias())) {
            throw new InvalidPersonException("The alias is already in use on a sheet of this sheet's tree.");
        }
        aliases.saveAndFlush(new PersonAlias(person.getSheetId(), person.getId(), input.alias()));
    }
}
