package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Checks, writing nothing, that the signed-in user's settle of every one of the claims would go through: each must
 * be on the sheet and pass the claim's own settle checks (its period open, then every refusal of the settle action).
 * Going PENDING is not a refusal. Any claim refused refuses the whole bulk settle (400).
 */
public class CheckBulkSettle {

    /** The bulk settle may go ahead: how many claims it will settle. */
    public record BulkSettleCheck(int count) {
    }

    public void check(@PathVariable("sheetId") String sheetId, @RequestParam("user") String user,
            @RequestBody BulkSettleRequest request, ClaimRepository claims, SheetRepository sheets,
            PersonRepository people, RateRepository rates, SettingRepository settings, PeriodRepository periods,
            VoidReasonRepository voidReasons, ClaimLogRepository logs, ObjectResponse<BulkSettleCheck> response)
            throws InvalidClaimException {
        List<Long> ids = request == null || request.claimIds() == null ? List.of() : request.claimIds();
        ClaimActionRequest settle = new ClaimActionRequest("", "");
        for (Long id : ids) {
            Claim claim = id == null ? null : claims.find(sheetId, id);
            if (claim == null) {
                throw new InvalidClaimException("No such claim on the sheet: " + id);
            }
            new CheckClaimPeriodOpen().check(claim, periods);
            ApplyClaimAction.decide("settle", user, claim, settle, sheets, people,
                    rates, settings, periods, voidReasons, claims, logs, null, (a) -> {
                    }, (n) -> {
                    }, (p) -> {
                    }, (c) -> {
                    }, false);
        }
        response.send(new BulkSettleCheck(ids.size()));
    }
}
