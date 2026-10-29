package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.Map;
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
    // decimals, more than 0.00 and at most 100.00. A member pool (memberPool) must be of the sheet, not the pool itself nor
    // one containing it through any chain, not already a member of a pool, and the chain it makes at most three deep.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PoolMemberRequest body, Pools pools,
            People people, Out<PoolMember> member) throws PoolRefusedException {
        String sheet = sheetId.trim();
        String pool = body == null || body.pool() == null ? "" : body.pool().trim();
        if (!pools.has(sheet, pool)) {
            throw new PoolRefusedException("Choose a pool.");
        }
        String memberPool = body.memberPool() == null ? "" : body.memberPool().trim();
        Person person = null;
        if (!memberPool.isEmpty()) {
            nested(sheet, pool, memberPool, pools);
        } else {
            person = body.personId() == null ? null : people.find(sheet, body.personId());
            if (person == null) {
                throw new PoolRefusedException("Choose a person.");
            }
            if (pools.isMember(sheet, pool, person.id())) {
                throw new PoolRefusedException(person.name() + " is already in the pool.");
            }
        }
        String typed = body.share() == null ? "" : body.share().trim();
        if (!SHARE.matcher(typed).matches()) {
            throw new PoolRefusedException("Give the share as a percent with at most two decimals.");
        }
        BigDecimal share = new BigDecimal(typed).setScale(2);
        if (share.signum() <= 0 || share.compareTo(HUNDRED) > 0) {
            throw new PoolRefusedException("The share must be more than 0.00 and at most 100.00.");
        }
        member.set(person == null ? new PoolMember(sheet, pool, 0, memberPool, share, memberPool)
                : new PoolMember(sheet, pool, person.id(), person.name(), share, null));
    }

    private static final int DEEPEST = 3;

    private static void nested(String sheet, String pool, String memberPool, Pools pools) throws PoolRefusedException {
        if (!pools.has(sheet, memberPool)) {
            throw new PoolRefusedException("Choose a pool.");
        }
        if (memberPool.equals(pool)) {
            throw new PoolRefusedException("A pool cannot be a member of itself.");
        }
        Map<String, String> parents = pools.parents(sheet);
        if (parents.containsKey(memberPool)) {
            throw new PoolRefusedException(memberPool + " is already a member of " + parents.get(memberPool) + ".");
        }
        int above = 1;
        for (String p = parents.get(pool); p != null; p = parents.get(p)) {
            if (p.equals(memberPool)) {
                throw new PoolRefusedException(memberPool + " already contains " + pool + ".");
            }
            above++;
        }
        if (above + depth(memberPool, parents) > DEEPEST) {
            throw new PoolRefusedException("A chain of pools is at most " + DEEPEST + " deep.");
        }
    }

    /** How many levels deep the pool's own chain goes, itself counting 1. */
    private static int depth(String pool, Map<String, String> parents) {
        int deepest = 0;
        for (Map.Entry<String, String> e : parents.entrySet()) {
            if (e.getValue().equals(pool)) {
                deepest = Math.max(deepest, depth(e.getKey(), parents));
            }
        }
        return deepest + 1;
    }
}
