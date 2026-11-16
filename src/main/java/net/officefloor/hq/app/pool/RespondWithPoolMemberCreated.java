package net.officefloor.hq.app.pool;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithPoolMemberCreated {
    public void respond(@Val PoolMemberValues values, ObjectResponse<ResponseEntity<PoolMember>> response) {
        response.send(new ResponseEntity<>(new PoolMember(values.personId(), values.name(),
                values.isPool() ? values.name() : null, values.share(), false, null), HttpStatus.CREATED));
    }
}
