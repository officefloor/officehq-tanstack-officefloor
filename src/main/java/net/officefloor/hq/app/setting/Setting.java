package net.officefloor.hq.app.setting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A sheet's setting by key; its value is null when not set (for clerk-limit: no limit). */
@Entity
@Table(name = "setting")
public class Setting {

    /** The clerk limit: a clerk settles only a claim whose due is at or below it (two decimals). */
    public static final String CLERK_LIMIT = "clerk-limit";

    /** The floor: a claim's live due is at least it (two decimals); 1.00 when not set. */
    public static final String FLOOR = "floor";

    /** The terms: the number of days after a claim's date by which it is due (14 when not set). */
    public static final String TERMS = "terms";

    /** The late percent: an overdue claim's live due gains it of its floored due for every full 30 days
     *  it is overdue (two decimals); 0.00 when not set. */
    public static final String LATE_PCT = "late-pct";

    /** The policy date (yyyy-mm-dd): a claim dated before it rounds its live figures half up, one dated on or
     *  after it half to even; the sheet's today when not set. */
    public static final String POLICY_DATE = "policy-date";

    /** The late percent in force when none is set. */
    public static final String DEFAULT_LATE_PCT = "0.00";

    /** The terms in force when none is set. */
    public static final String DEFAULT_TERMS = "14";

    /** The floor in force when none is set. */
    public static final String DEFAULT_FLOOR = "1.00";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "setting_key", nullable = false)
    private String key;

    @Column(name = "setting_value")
    private String value;

    protected Setting() {
    }

    public Setting(String sheetId, String key) {
        this.sheetId = sheetId;
        this.key = key;
    }

    public String getSheetId() {
        return sheetId;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
