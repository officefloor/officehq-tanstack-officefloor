package net.officefloor.hq.app.person;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.hq.app.statement.Statement;
import net.officefloor.hq.app.statement.StatementRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeople {
    public void respond(@Val Sheet sheet, PersonRepository repository, StatementRepository statements,
            PatronRepository patrons, PersonGradeRepository grades, ObjectResponse<List<PersonResponse>> response) {
        Map<Long, List<Statement>> byPerson = statements.findBySheetIdOrderByIdAsc(sheet.getId()).stream()
                .collect(Collectors.groupingBy(Statement::getPersonId));
        Map<Long, List<Patron>> patronsOf = patrons.findBySheetIdOrderByIdAsc(sheet.getId()).stream()
                .collect(Collectors.groupingBy(Patron::getPersonId));
        Map<Long, List<PersonGrade>> gradesOf = grades.datedOnSheet(sheet.getId());
        response.send(repository.findBySheetIdOrderByIdAsc(sheet.getId()).stream()
                .map(p -> PersonResponse.from(p, byPerson.getOrDefault(p.getId(), List.of()),
                        patronsOf.getOrDefault(p.getId(), List.of()),
                        new PersonGrades(p.getGrade(), gradesOf.getOrDefault(p.getId(), List.of())), sheet.getToday()))
                .toList());
    }
}
