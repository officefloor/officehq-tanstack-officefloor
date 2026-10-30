package net.officefloor.hq.app.opening;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.PeriodResponse;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Takes the imported openings in order. Every line is checked against the sheet first (the person is on
 * the sheet, by name or alias, matched as names are compared; the period is on the sheet); one refused
 * line takes none. Each line then adds its amount to the person's opening for the period: OPENING_SET.
 */
public class SaveImportOpenings {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidImportOpenings request, People people,
            Periods periods, Openings openings, Audit audit, ObjectResponse<ImportOpeningsResponse> response)
            throws InvalidOpeningException {
        List<PeriodResponse> onSheet = periods.list(sheetId);
        List<Person> persons = new ArrayList<>();
        int number = 0;
        for (ValidImportOpenings.Line line : request.lines()) {
            number++;
            Person person = people.findByName(sheetId, line.name());
            if (person == null) {
                throw new InvalidOpeningException("Line " + number + ": no one on the sheet is named " + line.name());
            }
            if (onSheet.stream().noneMatch(p -> p.name().equals(line.period()))) {
                throw new InvalidOpeningException("Line " + number + ": no period on the sheet is named " + line.period());
            }
            persons.add(person);
        }
        for (int i = 0; i < request.lines().size(); i++) {
            ValidImportOpenings.Line line = request.lines().get(i);
            Person person = persons.get(i);
            openings.add(sheetId, person.getId(), line.period(), line.amount());
            audit.recordOnCommit("OPENING_SET sheet=" + sheetId + " by=" + request.user() + " name=" + person.getName()
                    + " period=" + line.period() + " amount=" + line.amount().toPlainString());
        }
        response.send(new ImportOpeningsResponse(request.lines().size()));
    }
}
