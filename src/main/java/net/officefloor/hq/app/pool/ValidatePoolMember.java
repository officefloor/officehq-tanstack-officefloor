package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePoolMember {

    private static final Pattern SHARE = Pattern.compile("\\d+(\\.\\d{1,2})?");

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    // The pool and the person must be of the sheet, the person not already in the pool; the share has at most two
    // decimals, more than 0.00 and at most 100.00.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PoolMemberRequest body, Pools pools,
            People people, Out<PoolMember> member) throws PoolRefusedException {
        String sheet = sheetId.trim();
        String pool = body == null || body.pool() == null ? "" : body.pool().trim();
        if (!pools.has(sheet, pool)) {
            throw new PoolRefusedException("Choose a pool.");
        }
        Person person = body.personId() == null ? null : people.find(sheet, body.personId());
        if (person == null) {
            throw new PoolRefusedException("Choose a person.");
        }
        if (pools.isMember(sheet, pool, person.id())) {
            throw new PoolRefusedException(person.name() + " is already in the pool.");
        }
        String typed = body.share() == null ? "" : body.share().trim();
        if (!SHARE.matcher(typed).matches()) {
            throw new PoolRefusedException("Give the share as a percent with at most two decimals.");
        }
        BigDecimal share = new BigDecimal(typed).setScale(2);
        if (share.signum() <= 0 || share.compareTo(HUNDRED) > 0) {
            throw new PoolRefusedException("The share must be more than 0.00 and at most 100.00.");
        }
        member.set(new PoolMember(sheet, pool, person.id(), person.name(), share));
    }
}
