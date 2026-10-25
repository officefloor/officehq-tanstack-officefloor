package net.officefloor.hq.app.grade;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** The grades, and the rules for a retired one: people on it keep it, but it cannot be given to a new person or in an
 *  edit, and a default grade that is retired falls back to A. */
public final class RetiredGrades {

    public static final List<String> GRADES = List.of("A", "B", "C");

    private RetiredGrades() {
    }

    /** The sheet's retired grades. */
    public static Set<String> of(String sheetId, RetiredGradeRepository repository) {
        return repository.findBySheetId(sheetId).stream().map(RetiredGrade::getGrade).collect(Collectors.toSet());
    }

    public static boolean isRetired(String sheetId, String grade, RetiredGradeRepository repository) {
        return repository.existsBySheetIdAndGrade(sheetId, grade);
    }

    /** The grade a person is added with when none is chosen: the sheet's default grade, or A when it is retired. */
    public static String defaultGrade(String sheetId, String defaultGrade, RetiredGradeRepository repository) {
        return isRetired(sheetId, defaultGrade, repository) ? "A" : defaultGrade;
    }
}
