package net.officefloor.hq.app.period;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Finds every line's person (by name or alias) and period on the sheet before any is set, so one
 *  line naming neither refuses the paste and writes nothing; then adds each line's amount, in order,
 *  to the person's opening balance for the period, audited OPENING_SET. */
public class SetImportedOpenings {
    public void set(@PathVariable("sheetId") String sheetId, @Val ImportedOpenings imported, @Val Actor actor,
            People people, Periods periods, Audit audit, Out<ImportOpeningsResult> result)
            throws InvalidSheetRequestException {
        List<Person> persons = new ArrayList<>();
        List<Period> found = new ArrayList<>();
        for (OpeningLine line : imported.lines()) {
            Person person = people.named(sheetId, line.name());
            if (person == null) {
                throw new InvalidSheetRequestException("Line " + line.line() + ": no one on the sheet is named " + line.name());
            }
            Period period = periods.find(sheetId, line.period());
            if (period == null) {
                throw new InvalidSheetRequestException("Line " + line.line() + ": no period on the sheet is named " + line.period());
            }
            persons.add(person);
            found.add(period);
        }
        for (int i = 0; i < imported.lines().size(); i++) {
            periods.addOpening(found.get(i), persons.get(i).getId(), imported.lines().get(i).amount());
        }
        for (int i = 0; i < imported.lines().size(); i++) {
            audit.record("OPENING_SET sheet=" + sheetId + " by=" + actor.user() + " name=" + persons.get(i).getName()
                    + " period=" + found.get(i).name() + " amount=" + imported.lines().get(i).amount().toPlainString());
        }
        result.set(new ImportOpeningsResult(imported.lines().size()));
    }
}
