package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;

/** Refuses a name already used by another person on the sheet (ignoring case; names are trimmed). */
public class CheckPersonName {
    public void check(@Val PersonRequest request, @Val Person person, PersonRepository repository)
            throws InvalidPersonException {
        boolean taken = repository.findBySheetIdOrderByIdAsc(person.getSheetId()).stream()
                .anyMatch((other) -> !other.getId().equals(person.getId())
                        && other.getName().equalsIgnoreCase(request.name()));
        if (taken) {
            throw new InvalidPersonException("The name " + request.name() + " is already in use on this sheet.");
        }
    }
}
