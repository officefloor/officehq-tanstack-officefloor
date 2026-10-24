package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonGradeRepository extends JpaRepository<PersonGrade, Long> {

    /** The sheet's dated grades, by from-date. */
    List<PersonGrade> findBySheetIdOrderByFromDateAsc(String sheetId);

    /** The person's dated grades, by from-date. */
    List<PersonGrade> findByPersonIdOrderByFromDateAsc(Long personId);

    Optional<PersonGrade> findByPersonIdAndFromDate(Long personId, LocalDate fromDate);

    /** The person's grades over time. */
    default PersonGrades gradesOf(Person person) {
        return new PersonGrades(person.getGrade(), findByPersonIdOrderByFromDateAsc(person.getId()));
    }

    /** The grades over time of each person on the sheet, by person id; a person with no dated grade is absent. */
    default Map<Long, List<PersonGrade>> datedOnSheet(String sheetId) {
        return findBySheetIdOrderByFromDateAsc(sheetId).stream()
                .collect(Collectors.groupingBy(PersonGrade::getPersonId));
    }
}
