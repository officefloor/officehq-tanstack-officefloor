package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildPerson {
    public void build(@Val PersonRequest request, @Val Sheet sheet, PersonRepository repository,
            PersonAliasRepository aliases, TreeNames tree, Out<Person> personOut)
            throws InvalidPersonException {
        if (repository.existsBySheetIdAndNameIgnoreCase(sheet.getId(), request.name())
                || aliases.existsBySheetIdAndAliasIgnoreCase(sheet.getId(), request.name())) {
            throw new InvalidPersonException("The name is already in use on this sheet.");
        }
        if (tree.usedAround(sheet.getId(), request.name())) {
            throw new InvalidPersonException("The name is already in use on a sheet of this sheet's tree.");
        }
        if (request.contactOrNull() != null
                && repository.existsBySheetIdAndContact(sheet.getId(), request.contactOrNull())) {
            throw new InvalidPersonException("The contact is already in use on this sheet.");
        }
        Person person = new Person(sheet.getId(), request.name(), request.grade());
        person.setAddedOn(sheet.getToday());
        person.setTerms(request.termsDays());
        person.setContact(request.contactOrNull());
        personOut.set(repository.save(person));
    }
}
