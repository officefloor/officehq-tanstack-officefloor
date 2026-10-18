package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.stereotype.Service;

/** Writes a claim's status log: once a saved claim's status differs from the one it was loaded with (a new
 *  claim has none, so its DRAFT is logged), a line with that status, the user, the sheet's today and the
 *  claim's due as computed now (see {@link ClaimDue}). */
@Service
public class ClaimLog {
    private final ClaimLogRepository log;
    private final PersonRepository people;
    private final RateRepository rates;
    private final SettingRepository settings;
    private final SheetRepository sheets;

    public ClaimLog(ClaimLogRepository log, PersonRepository people, RateRepository rates, SettingRepository settings,
            SheetRepository sheets) {
        this.log = log;
        this.people = people;
        this.rates = rates;
        this.settings = settings;
        this.sheets = sheets;
    }

    void record(Claim claim, String user) {
        if (claim.getStatus() == claim.getLoggedStatus()) {
            return;
        }
        LocalDate today = sheets.findById(claim.getSheetId()).orElseThrow().getToday();
        log.save(new ClaimLogEntry(claim, user == null ? "" : user.trim(), today,
                ClaimDue.now(claim, people, rates, settings, sheets)));
        claim.markLogged();
    }
}
