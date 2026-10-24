package net.officefloor.hq.app.sheet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

/** One setting set on a sheet, as it was recorded (the policy date and period numbering from by their keys), with
 *  the sheet's today then. */
@Entity
@Table(name = "setting_log")
public class SettingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sheet_id", nullable = false)
    private String sheetId;

    @Column(name = "setting_key", nullable = false)
    private String key;

    @Column(name = "from_date")
    private LocalDate fromDate;

    @Column(name = "setting_value")
    private String value;

    @Column(name = "made_on", nullable = false)
    private LocalDate madeOn;

    protected SettingLog() {
    }

    public SettingLog(String sheetId, String key, LocalDate fromDate, String value, LocalDate madeOn) {
        this.sheetId = sheetId;
        this.key = key;
        this.fromDate = fromDate;
        this.value = value;
        this.madeOn = madeOn;
    }

    public String getKey() {
        return key;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public String getValue() {
        return value;
    }

    public LocalDate getMadeOn() {
        return madeOn;
    }
}
