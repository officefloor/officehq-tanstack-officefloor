package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the person to the pool with their share; refuses a person not on the sheet, or one already in the pool. */
public class ApplyPoolMember {
    public void apply(@Val ValidPoolMember request, @Val Pool pool, PersonRepository people,
            PoolMemberRepository members, Out<PoolMember> memberOut, Out<Person> personOut) throws InvalidPoolException {
        Person person = people.findByIdAndSheetId(request.personId(), pool.getSheetId())
                .orElseThrow(() -> new InvalidPoolException("There is no such person on the sheet."));
        if (members.existsByPoolIdAndPersonId(pool.getId(), person.getId())) {
            throw new InvalidPoolException("That person is already in the pool.");
        }
        personOut.set(person);
        memberOut.set(members.saveAndFlush(new PoolMember(pool.getId(), person.getId(), request.share())));
    }
}
