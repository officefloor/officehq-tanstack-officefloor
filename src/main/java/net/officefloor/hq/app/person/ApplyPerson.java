package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, PersonRepository repository)
            throws InvalidPersonException {
        if (repository.existsBySheetIdAndNameIgnoreCaseAndIdNot(person.getSheetId(), request.name(), person.getId())) {
            throw new InvalidPersonException("The name is already in use on this sheet.");
        }
        person.setName(request.name());
        person.setGrade(request.grade());
        repository.saveAndFlush(person);
    }
}
