package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/** The sheet's floor: the least live due a claim has (two decimals). It is 1.00 until it is set.
 *  A settled claim's frozen figure, a statement's bound figure and a closed period's total keep the
 *  figure they were fixed at. */
@Service
public class Floor {

    public static final String KEY = "floor";

    public static final String DEFAULT = "1.00";

    private final SettingRepository settings;

    public Floor(SettingRepository settings) {
        this.settings = settings;
    }

    /** The floor in force on the sheet. */
    public BigDecimal of(String sheetId) {
        return new BigDecimal(settings.find(sheetId, KEY).orElse(DEFAULT));
    }

    /** The live due raised to the floor; null (n/a) stays null. */
    public static BigDecimal raise(BigDecimal due, BigDecimal floor) {
        return due == null || due.compareTo(floor) >= 0 ? due : floor;
    }
}
