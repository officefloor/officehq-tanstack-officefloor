package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a member by a viewer, of a pool or person not on the sheet, of a person already in the pool, or
 *  with a share that is not a number of at most two decimals, more than 0.00 and at most 100.00. */
public class ValidateMember {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void validate(@PathVariable("sheetId") String sheetId, @PathVariable("poolId") String poolId,
            @RequestBody MemberRequest request, Out<MemberInput> body, RoleStore roles, PoolStore pools,
            PersonStore people) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheet, user);
        PoolStore.Pool pool;
        try {
            pool = pools.find(sheet, Long.parseLong(poolId.trim()));
        } catch (NumberFormatException e) {
            pool = null;
        }
        if (pool == null) {
            throw new InvalidSheetException("No such pool");
        }
        Person person = request == null || request.personId() == null ? null : people.find(sheet, request.personId());
        if (person == null) {
            throw new InvalidSheetException("A person is required");
        }
        String text = request.share() == null ? "" : request.share().trim();
        if (!text.matches("\\d+(\\.\\d{1,2})?")) {
            throw new InvalidSheetException("A share is a percent with at most two decimals");
        }
        BigDecimal share = new BigDecimal(text).setScale(2);
        if (share.signum() <= 0 || share.compareTo(HUNDRED) > 0) {
            throw new InvalidSheetException("A share is more than 0.00 and at most 100.00");
        }
        if (pools.isMember(pool.id(), person.getId())) {
            throw new InvalidSheetException(person.getName() + " is already in " + pool.name());
        }
        body.set(new MemberInput(pool, person.getId(), person.getName(), share, user));
    }
}
