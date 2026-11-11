package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A share needs a person and a percent with up to two decimals, more than 0.00. */
public class ValidateShare {
    public void validate(@RequestBody ShareRequest request, Out<Sharing> body) throws InvalidClaimException {
        String person = request == null || request.personId() == null ? "" : request.personId().trim();
        String share = request == null || request.share() == null ? "" : request.share().trim();
        if (!person.matches("\\d{1,18}")) throw new InvalidClaimException("A share needs a person");
        if (!share.matches("\\d{1,3}(\\.\\d{1,2})?")) {
            throw new InvalidClaimException("A share needs a percent with up to two decimals");
        }
        BigDecimal pct = new BigDecimal(share).setScale(2);
        if (pct.signum() <= 0) throw new InvalidClaimException("A share must be more than 0.00");
        body.set(new Sharing(Long.parseLong(person), pct));
    }
}
