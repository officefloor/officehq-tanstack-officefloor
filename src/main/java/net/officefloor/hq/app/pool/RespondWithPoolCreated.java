package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class RespondWithPoolCreated {
    public void respond(@Val PoolValues values, ObjectResponse<ResponseEntity<Pool>> response) {
        response.send(new ResponseEntity<>(new Pool(values.name(), List.of(), new BigDecimal("0.00"), false),
                HttpStatus.CREATED));
    }
}
