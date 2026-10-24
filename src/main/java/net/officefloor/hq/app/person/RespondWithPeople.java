package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.sheet.InvalidTodayException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.statement.Statement;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's people; as of a date (asOf), as they stood at the end of it. */
public class RespondWithPeople {
    public void respond(@Val Sheet sheet, @RequestParam(name = "asOf", required = false) String asOfParam, AsOf asOf,
            ObjectResponse<List<PersonResponse>> response) throws InvalidTodayException {
        LocalDate date = AsOf.dateOf(asOfParam);
        Sheet view = asOf.sheet(sheet, date);
        Map<Long, List<Statement>> byPerson = asOf.statements(sheet.getId(), date).stream()
                .collect(Collectors.groupingBy(Statement::getPersonId));
        Map<Long, List<Patron>> patronsOf = asOf.patrons(sheet.getId(), date).stream()
                .collect(Collectors.groupingBy(Patron::getPersonId));
        Map<Long, List<PersonAlias>> aliasesOf = asOf.aliases(sheet.getId(), date).stream()
                .collect(Collectors.groupingBy(PersonAlias::getPersonId));
        Map<Long, List<PersonGrade>> gradesOf = asOf.grades(sheet.getId(), date);
        response.send(asOf.people(sheet.getId(), date).stream()
                .map(p -> PersonResponse.from(p, byPerson.getOrDefault(p.getId(), List.of()),
                        patronsOf.getOrDefault(p.getId(), List.of()),
                        aliasesOf.getOrDefault(p.getId(), List.of()),
                        new PersonGrades(p.getGrade(), gradesOf.getOrDefault(p.getId(), List.of())), view.getToday()))
                .toList());
    }
}
