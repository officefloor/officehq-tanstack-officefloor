package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Service;

/**
 * Writes a claim's status log: once the claim is stored with its new status (DRAFT when added), a line with that
 * status, who made the change, the sheet's today, and the claim's due as the screen shows it at that moment (its
 * figure with its late charge, see ClaimDue and LateCharge; n/a when it has none).
 */
@Service
public class ClaimLog {

    private final ClaimLogRepository log;
    private final ClaimRepository claims;
    private final PersonRepository people;
    private final RateRepository rates;
    private final SettingRepository settings;
    private final SheetRepository sheets;

    public ClaimLog(ClaimLogRepository log, ClaimRepository claims, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets) {
        this.log = log;
        this.claims = claims;
        this.people = people;
        this.rates = rates;
        this.settings = settings;
        this.sheets = sheets;
    }

    /** The sheet's today on the claim's latest SETTLED line of its status log (null when it has none). */
    public LocalDate settledOn(Claim claim) {
        LocalDate on = null;
        for (ClaimLogLine line : log.of(claim.sheetId(), claim.id())) {
            if (line.status() == ClaimStatus.SETTLED) {
                on = line.date();
            }
        }
        return on;
    }

    /** The sheet's today on the claim's latest DELETED line of its status log (null when it has none). */
    public LocalDate deletedOn(Claim claim) {
        LocalDate on = null;
        for (ClaimLogLine line : log.of(claim.sheetId(), claim.id())) {
            if (line.status() == ClaimStatus.DELETED) {
                on = line.date();
            }
        }
        return on;
    }

    public void write(Claim claim, String by) {
        String sheetId = claim.sheetId();
        LateCharge late = LateCharge.of(sheetId, sheets, settings, claims);
        Person person = people.find(sheetId, claim.personId());
        BigDecimal figure = person == null ? claim.due()
                : late.add(claim, ClaimDue.figure(claim, person.gradeOn(claim.date()), rates, late.rounding()),
                        settings.floor(sheetId, claim.date()));
        log.add(sheetId, new ClaimLogLine(claim.id(), claim.status(), by, late.today(),
                figure == null ? "n/a" : figure.toPlainString()));
    }
}
