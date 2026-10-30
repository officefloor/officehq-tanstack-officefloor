package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePoolMember {

    /** A share percent: at most two decimals. */
    private static final Pattern SHARE = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody PoolMemberRequest request,
            Out<ValidPoolMember> body, Settings settings, Pools pools, People people)
            throws InvalidPoolException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String pool = request.pool() == null ? "" : request.pool();
        if (!pools.contains(sheetId, pool)) {
            throw new InvalidPoolException("No such pool");
        }
        long personId;
        try {
            personId = Long.parseLong(request.personId() == null ? "" : request.personId().trim());
        } catch (NumberFormatException e) {
            throw new InvalidPoolException("Choose a person");
        }
        Person person = people.find(sheetId, personId);
        if (person == null) {
            throw new InvalidPoolException("No such person");
        }
        if (pools.hasMember(sheetId, pool, personId)) {
            throw new InvalidPoolException("The person is already in the pool");
        }
        String text = request.share() == null ? "" : request.share().trim();
        if (!SHARE.matcher(text).matches()) {
            throw new InvalidPoolException("A share is a percent with at most two decimals");
        }
        BigDecimal share = new BigDecimal(text).setScale(2);
        if (share.signum() <= 0 || share.compareTo(Pools.FULL) > 0) {
            throw new InvalidPoolException("A share is more than 0.00 and at most 100.00");
        }
        body.set(new ValidPoolMember(pool, personId, person.getName(), share, request.user()));
    }
}
