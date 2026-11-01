package net.officefloor.hq.app.grade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The grades of each sheet (A, B and C, then those added to it) and which of them are retired: a
 *  retired grade cannot be chosen for a person, though the people already on it keep it. */
@Repository
public class Grades {

    /** The grades every sheet has, in order. */
    public static final List<String> ALL = List.of("A", "B", "C");

    /** The form of a grade's code: one to three capital letters. */
    public static final Pattern CODE = Pattern.compile("[A-Z]{1,3}");

    /** The grade a choice falls back to when the one it would take is retired. */
    public static final String FALLBACK = "A";

    private final JdbcTemplate jdbc;

    public Grades(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every grade of the sheet, in order: A, B and C, then those added, in the order they were added.
     *  Read from the live tables, so the grades are the same when the sheet is viewed as of a date. */
    public List<String> all(String sheetId) {
        List<String> all = new ArrayList<>(ALL);
        all.addAll(jdbc.queryForList("SELECT grade FROM PUBLIC.sheet_grade WHERE sheet_id = ? ORDER BY seq",
                String.class, sheetId));
        return all;
    }

    public boolean isGrade(String sheetId, String grade) {
        return grade != null && all(sheetId).contains(grade);
    }

    /** Refused when the grade is not one of the sheet's. */
    public void refuseUnknown(String sheetId, String grade) throws InvalidSheetRequestException {
        if (!isGrade(sheetId, grade)) {
            throw new InvalidSheetRequestException("The grade " + grade + " is not one of the sheet's");
        }
    }

    public void add(String sheetId, String grade) {
        jdbc.update("INSERT INTO PUBLIC.sheet_grade (sheet_id, grade) VALUES (?, ?)", sheetId, grade);
    }

    /** The sheet's retired grades. */
    public Set<String> retired(String sheetId) {
        return new TreeSet<>(jdbc.queryForList("SELECT grade FROM grade_retired WHERE sheet_id = ?", String.class, sheetId));
    }

    public boolean isRetired(String sheetId, String grade) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM grade_retired WHERE sheet_id = ? AND grade = ?",
                Integer.class, sheetId, grade);
        return n != null && n > 0;
    }

    /** The grade, or the fallback when it is retired on the sheet. */
    public String orFallback(String sheetId, String grade) {
        return grade != null && isRetired(sheetId, grade) ? FALLBACK : grade;
    }

    /** Refused when the grade is not one of the sheet's, or is retired on it. */
    public void refuseRetired(String sheetId, String grade) throws InvalidSheetRequestException {
        if (grade != null) refuseUnknown(sheetId, grade);
        if (grade != null && isRetired(sheetId, grade)) {
            throw new InvalidSheetRequestException("The grade " + grade + " is retired");
        }
    }

    public void retire(String sheetId, String grade) {
        jdbc.update("INSERT INTO grade_retired (sheet_id, grade) VALUES (?, ?)", sheetId, grade);
    }
}
