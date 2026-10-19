package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/** The sheet's terms: the whole number of days after its date a claim is due by. It is 14 until it is
 *  set. Each grade may have terms of its own ({@code terms-<grade>}, blank until set); a grade without
 *  them uses the sheet's. A claim's due-by is fixed when it is created, from the terms of its person's
 *  grade in force on its date, and does not change when the terms or the claim's date change later. */
@Service
public class Terms {

    public static final String KEY = "terms";

    public static final String DEFAULT = "14";

    public static final List<String> GRADES = List.of("A", "B", "C");

    private final SettingRepository settings;

    public Terms(SettingRepository settings) {
        this.settings = settings;
    }

    /** The key of the grade's own terms. */
    public static String keyOf(String grade) {
        return KEY + "-" + grade;
    }

    /** Whether the key is that of a grade's own terms. */
    public static boolean isGradeKey(String key) {
        return GRADES.stream().anyMatch(g -> keyOf(g).equals(key));
    }

    /** The terms in force for the grade: its own, else the sheet's. */
    public int of(String sheetId, String grade) {
        return Integer.parseInt(settings.find(sheetId, keyOf(grade))
                .or(() -> settings.find(sheetId, KEY)).orElse(DEFAULT));
    }

    /** The due-by of a claim dated {@code date} for a person of {@code grade} on that date. */
    public LocalDate dueBy(String sheetId, String grade, LocalDate date) {
        return date.plusDays(of(sheetId, grade));
    }
}
