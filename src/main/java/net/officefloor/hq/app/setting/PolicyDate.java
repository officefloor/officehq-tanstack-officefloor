package net.officefloor.hq.app.setting;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.stereotype.Service;

/** The sheet's policy date (yyyy-MM-dd; blank until set, when it is the sheet's today). A claim dated
 *  before it rounds its live figures (the rate step and the late charge) half up, one dated on or after it
 *  half to even. A correction rounds by the date of the claim it corrects. Settled and bound figures keep
 *  the figure they were fixed at. */
@Service
public class PolicyDate {

    public static final String KEY = "policy-date";

    private final SettingRepository settings;
    private final SheetClock clock;
    private final ClaimRepository claims;

    public PolicyDate(SettingRepository settings, SheetClock clock, ClaimRepository claims) {
        this.settings = settings;
        this.clock = clock;
        this.claims = claims;
    }

    /** The rounding policy as it stands on the sheet now. */
    public Policy of(String sheetId) {
        LocalDate date = settings.find(sheetId, KEY).map(LocalDate::parse).orElseGet(() -> clock.today(sheetId));
        return new Policy(date, claims.findBySheet(sheetId));
    }

    /** The policy date and the sheet's claims, which a correction's corrected claim is found among. */
    public record Policy(LocalDate date, List<Claim> claims) {

        /** How the claim's live figures round. */
        public RoundingMode modeFor(Claim claim) {
            return roundsOn(claim).isBefore(date) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
        }

        /** The claim's date, or for a correction the date of the claim it corrects. */
        private LocalDate roundsOn(Claim claim) {
            if (!claim.isCorrection()) return claim.getDate();
            return claims.stream()
                    .filter(c -> c.getPersonId() == claim.getPersonId() && claim.getCorrects().equals(c.getNumber()))
                    .map(Claim::getDate).findFirst().orElse(claim.getDate());
        }
    }
}
