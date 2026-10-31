package net.officefloor.hq.app.pool;

import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithPools {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, Pools pools, AsOf asOfSheet,
            ObjectResponse<List<PoolResponse>> response) throws InvalidSheetRequestException {
        response.send(asOfSheet.view(sheetId, asOf, () -> pools.onSheet(sheetId)));
    }
}
