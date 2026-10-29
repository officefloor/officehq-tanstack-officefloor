package net.officefloor.hq.app.rate;

import java.util.Objects;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRate {
    // The rate as stored, with the version it was set as.
    public void respond(@Val Rate rate, Rates rates, ObjectResponse<RateResponse> response) {
        Rate stored = rates.list(rate.sheetId()).stream()
                .filter(r -> r.grade().equals(rate.grade()) && Objects.equals(r.from(), rate.from()))
                .findFirst().orElse(rate);
        response.send(RateResponse.from(stored));
    }
}
