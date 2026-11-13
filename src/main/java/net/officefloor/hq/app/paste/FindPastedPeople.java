package net.officefloor.hq.app.paste;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.claim.CheckClaimInputNotRetired;
import net.officefloor.hq.app.claim.CheckClaimInputPeriodOpen;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/**
 * Finds each line's person by name (as names are compared: ignoring case) and checks its claim against them with the
 * claim form's rules: no claim after the person's retirement date, none inside a closed period. Any line refused
 * refuses the whole paste, before anything is written.
 */
public class FindPastedPeople {
    public void find(@Val PastedLines pasted, @Val Sheet sheet, PersonRepository people, SheetRepository sheets,
            PeriodRepository periods) throws InvalidClaimException {
        List<PastedLine> found = new ArrayList<>();
        int number = 0;
        for (PastedLine line : pasted.lines()) {
            number++;
            Person person = people.findByName(sheet.id(), line.name());
            if (person == null) {
                throw new InvalidClaimException("Line " + number + ": " + line.name() + " is not on the sheet.");
            }
            try {
                new CheckClaimInputNotRetired().check(line.input(), person, sheet.id(), sheets);
                new CheckClaimInputPeriodOpen().check(line.input(), sheet.id(), sheets, periods);
            } catch (InvalidClaimException e) {
                throw new InvalidClaimException("Line " + number + ": " + e.getMessage());
            }
            found.add(new PastedLine(line.name(), line.input(), person));
        }
        pasted.lines().clear();
        pasted.lines().addAll(found);
    }
}
