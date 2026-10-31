package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a member for a pool the sheet does not have, a person not on the sheet or already in the
 *  pool, or a share that is not a percent with up to two decimals, more than 0.00 and at most 100.00. */
public class ValidatePoolMember {

    private static final Pattern SHARE = Pattern.compile("\\d{1,3}(\\.\\d{1,2})?");
    private static final BigDecimal MAX = new BigDecimal("100.00");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PoolMemberRequest request,
            Pools pools, Out<PoolMember> member) throws InvalidSheetRequestException {
        if (request == null || request.pool() == null || !pools.has(sheetId, request.pool())) {
            throw new InvalidSheetRequestException("No such pool");
        }
        String name = request.personId() == null ? null : pools.personName(sheetId, request.personId());
        if (name == null) {
            throw new InvalidSheetRequestException("No such person");
        }
        String text = request.share() == null ? "" : request.share().trim();
        if (!SHARE.matcher(text).matches()) {
            throw new InvalidSheetRequestException("A share is a percent with up to two decimals");
        }
        BigDecimal share = new BigDecimal(text).setScale(2);
        if (share.signum() <= 0 || share.compareTo(MAX) > 0) {
            throw new InvalidSheetRequestException("A share is more than 0.00 and at most 100.00");
        }
        if (pools.hasMember(sheetId, request.pool(), request.personId())) {
            throw new InvalidSheetRequestException(name + " is already in " + request.pool());
        }
        member.set(new PoolMember(sheetId, request.pool(), request.personId(), name, share, request.user()));
    }
}
