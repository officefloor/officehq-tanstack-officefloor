package net.officefloor.hq.app.importpeople;

import java.util.List;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonTermsChange;

/** The imported people, built and checked but not yet saved, in order. */
public record ImportedPersons(List<Entry> people) {

    /** A person to add (no terms change) or an existing person as the import updates them (with its terms change). */
    public record Entry(Person person, PersonTermsChange termsChange) {
        public boolean update() {
            return termsChange != null;
        }
    }
}
