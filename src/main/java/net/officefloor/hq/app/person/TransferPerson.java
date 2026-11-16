package net.officefloor.hq.app.person;

import net.officefloor.hq.app.claim.ClaimNumbering;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Stores the transfer (see PersonTransferrer#transfer), then gives each moved claim that had a number the next number
 *  of the other sheet (see ClaimNumbering), in the order the claims were opened. */
public class TransferPerson {
    public void transfer(@Val PersonTransfer transfer, PersonTransferrer transferrer, ClaimRepository claims,
            PeriodRepository periods, SettingRepository settings, SheetRepository sheets) {
        String to = transfer.toSheet();
        for (long id : transferrer.transfer(transfer.person(), to)) {
            claims.renumber(claims.find(to, id), ClaimNumbering.next(claims.find(to, id), claims, periods, settings, sheets));
        }
    }
}
