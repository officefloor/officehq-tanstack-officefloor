package net.officefloor.hq.app.delegation;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Store the delegation, refusing it unless made by a steward assigned the role (not one by delegation). */
public class SaveDelegation {
    public void save(@PathVariable("sheetId") String sheetId, @Val DelegationInput input, RoleRepository roles,
            DelegationRepository repository, Out<Delegation> delegation) throws InvalidDelegationException {
        String id = sheetId.trim();
        if (!Role.STEWARD.equals(roles.assigned(id, input.user()))) {
            throw new InvalidDelegationException("Only a steward can delegate the steward role.");
        }
        delegation.set(repository.add(id, input));
    }
}
