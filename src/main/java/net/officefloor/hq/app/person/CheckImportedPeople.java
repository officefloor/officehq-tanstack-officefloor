package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Checks every imported person before any is added, as the person form checks one (see CheckNewPersonName and
 *  CheckPersonContact): any failing line refuses the whole import. */
public class CheckImportedPeople {
    public void check(@PathVariable("sheetId") String sheetId, @Val ImportedPeople imported, PersonRepository people)
            throws DuplicatePersonNameException, DuplicatePersonContactException {
        String sheet = sheetId.trim();
        for (PersonValues values : imported.people()) {
            if (people.nameTaken(sheet, values.name(), 0) || people.treeTaken(sheet, values.name())) {
                throw new DuplicatePersonNameException(values.name());
            }
            if (values.contact() != null && people.contactTaken(sheet, values.contact(), 0)) {
                throw new DuplicatePersonContactException(values.contact());
            }
        }
    }
}
