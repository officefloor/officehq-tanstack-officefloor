package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with what the withdrawal left for the grade and from-date: the version that applies again, 0 when the
 *  rate is gone. */
public class RespondWithRateWithdrawn {
    public record Response(String grade, String from, int version) {
    }

    public void respond(@Val RateWithdrawn withdrawn, ObjectResponse<Response> response) {
        response.send(new Response(withdrawn.grade(), withdrawn.from() == null ? null : withdrawn.from().toString(),
                withdrawn.restored() == null ? 0 : withdrawn.restored().version()));
    }
}
