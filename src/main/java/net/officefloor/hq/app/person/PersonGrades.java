package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;

/** A person's grades over time: the first grade they were given applies from the beginning of time, each dated
 *  grade from its from-date on.
 *  @param first the grade they were first given.
 *  @param dated their dated grades, by from-date. */
public record PersonGrades(String first, List<PersonGrade> dated) {

    /** Their grades as they stood at the end of a date: only the dated grades recorded while the sheet's today was
     *  on or before it. */
    public PersonGrades asOf(LocalDate date) {
        return new PersonGrades(first, dated.stream().filter(g -> g.madeBy(date)).toList());
    }

    /** The grade in force on a date: the dated grade whose from-date is the latest on or before it, else the first. */
    public String on(LocalDate date) {
        String grade = first;
        for (PersonGrade g : dated) {
            if (date != null && !g.getFromDate().isAfter(date)) {
                grade = g.getGrade();
            }
        }
        return grade;
    }
}
