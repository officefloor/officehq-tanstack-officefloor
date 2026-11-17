package net.officefloor.hq.app.grade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.setting.SettingRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The grades A, B and C, the grades added to each sheet after them, and which of them are retired on each sheet. A retired grade cannot be chosen for a person:
 *  not for a new one, nor in an edit (one leaving the person's grade as it was, with no from-date, is not a choice of
 *  it); people already on it keep it. */
@Repository
public class GradeRepository {

    public static final List<String> GRADES = List.of("A", "B", "C");

    private final JdbcTemplate jdbc;

    public GradeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's grades: A, B and C, then those added to it, in the order they were added. */
    public List<String> grades(String sheetId) {
        List<String> grades = new ArrayList<>(GRADES);
        grades.addAll(jdbc.queryForList("SELECT grade FROM grade_added WHERE sheet_id = ? ORDER BY seq", String.class,
                sheetId));
        return grades;
    }

    public boolean isGrade(String sheetId, String grade) {
        return grades(sheetId).contains(grade);
    }

    public void add(String sheetId, String grade) {
        jdbc.update("INSERT INTO grade_added (sheet_id, grade) VALUES (?, ?)", sheetId, grade);
    }

    /** The grades retired on the sheet, in order. */
    public Set<String> retired(String sheetId) {
        return new TreeSet<>(jdbc.queryForList("SELECT grade FROM grade_retired WHERE sheet_id = ?", String.class, sheetId));
    }

    public boolean isRetired(String sheetId, String grade) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM grade_retired WHERE sheet_id = ? AND grade = ?",
                Integer.class, sheetId, grade);
        return n != null && n > 0;
    }

    public void retire(String sheetId, String grade) {
        jdbc.update("INSERT INTO grade_retired (sheet_id, grade) VALUES (?, ?)", sheetId, grade);
    }

    /** The grade a person added without one takes: the sheet's default grade (see SettingRepository#defaultGrade),
     *  falling back to A when that grade is retired. */
    public String defaultGrade(String sheetId, SettingRepository settings) {
        String grade = settings.defaultGrade(sheetId);
        return isRetired(sheetId, grade) ? "A" : grade;
    }

    /** Refuses the choice of a retired grade. */
    public void checkChoosable(String sheetId, String grade) throws InvalidPersonException {
        if (isRetired(sheetId, grade)) {
            throw new InvalidPersonException("Grade " + grade + " is retired");
        }
    }
}
