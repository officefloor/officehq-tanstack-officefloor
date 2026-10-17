package net.officefloor.hq.app.person;

import java.util.List;

/**
 * The people of an import that passed its checks, in the order they were pasted; those already on
 * the sheet (whose grade the import updates) are also listed in {@code updated}.
 */
public record ImportedPeople(List<Person> people, List<Person> updated) {

    public boolean isUpdated(Person person) {
        return updated.stream().anyMatch((p) -> p == person);
    }

    public int addedCount() {
        return people.size() - updated.size();
    }
}
