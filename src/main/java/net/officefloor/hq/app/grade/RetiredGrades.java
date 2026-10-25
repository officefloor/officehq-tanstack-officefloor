package net.officefloor.hq.app.grade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** The grades — A, B and C, then those added to the sheet in the order added — and the rules for a retired one:
 *  people on it keep it, but it cannot be given to a new person or in an edit, and a default grade that is retired
 *  falls back to A. */
public final class RetiredGrades {

    public static final List<String> GRADES = List.of("A", "B", "C");

    private static final Pattern CODE = Pattern.compile("[A-Z]{1,3}");

    private RetiredGrades() {
    }

    /** Whether the text is shaped as a grade: one to three capital letters. */
    public static boolean isCode(String grade) {
        return grade != null && CODE.matcher(grade).matches();
    }

    /** The sheet's grades: A, B and C, then those added to it in the order added. */
    public static List<String> all(String sheetId, AddedGradeRepository added) {
        List<String> grades = new ArrayList<>(GRADES);
        added.findBySheetIdOrderById(sheetId).forEach(g -> grades.add(g.getGrade()));
        return grades;
    }

    /** Whether the grade is one of the sheet's grades. */
    public static boolean isGrade(String sheetId, String grade, AddedGradeRepository added) {
        return GRADES.contains(grade) || added.existsBySheetIdAndGrade(sheetId, grade);
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
