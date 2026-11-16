package net.officefloor.hq.app.grade;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.person.InvalidPersonException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GradeRepository {

    /** The grades every sheet has; a sheet's added grades follow them. */
    public static final List<String> GRADES = List.of("A", "B", "C");

    /** The form of a grade's code: one to three capital letters. */
    public static final java.util.regex.Pattern CODE = java.util.regex.Pattern.compile("[A-Z]{1,3}");

    private final JdbcTemplate jdbc;

    public GradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's grades, in order, each with whether it is retired. */
    public List<Grade> list(String sheetId) {
        Set<String> retired = retired(sheetId);
        return codes(sheetId).stream().map((g) -> new Grade(g, retired.contains(g))).toList();
    }

    /** The sheet's grades, in order: A, B and C, then those added, in the order added. */
    public List<String> codes(String sheetId) {
        List<String> codes = new java.util.ArrayList<>(GRADES);
        codes.addAll(jdbc.queryForList("SELECT grade FROM grade_added WHERE sheet_id = ? ORDER BY id",
                String.class, sheetId));
        return codes;
    }

    /** Whether the grade is one of the sheet's grades. */
    public boolean isGrade(String sheetId, String grade) {
        return codes(sheetId).contains(grade);
    }

    public GradeAdded add(String sheetId, String grade, String by) {
        jdbc.update("INSERT INTO grade_added (sheet_id, grade, added_by, added_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                sheetId, grade, by);
        return new GradeAdded(sheetId, grade, by);
    }

    /** Adds to the sheet any of the grades it does not have yet (as a person brought from another
     *  sheet may be on one), quietly. */
    public void ensure(String sheetId, java.util.Collection<String> given, String by) {
        List<String> have = codes(sheetId);
        for (String g : new java.util.LinkedHashSet<>(given)) {
            if (g != null && !have.contains(g)) {
                add(sheetId, g, by);
            }
        }
    }

    public Set<String> retired(String sheetId) {
        return new HashSet<>(jdbc.queryForList("SELECT grade FROM grade_retired WHERE sheet_id = ?",
                String.class, sheetId));
    }

    public boolean isRetired(String sheetId, String grade) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM grade_retired WHERE sheet_id = ? AND grade = ?",
                Integer.class, sheetId, grade);
        return n != null && n > 0;
    }

    public GradeRetired retire(String sheetId, String grade, String by) {
        jdbc.update("INSERT INTO grade_retired (sheet_id, grade, retired_by, retired_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)",
                sheetId, grade, by);
        return new GradeRetired(sheetId, grade, by);
    }

    /** Refuses a grade chosen for a person (a new person, or an edit giving a grade) that is retired on the sheet. */
    public void requireChoosable(String sheetId, String grade) throws InvalidPersonException {
        if (isRetired(sheetId, grade)) {
            throw new InvalidPersonException("The grade is retired: " + grade);
        }
    }
}
