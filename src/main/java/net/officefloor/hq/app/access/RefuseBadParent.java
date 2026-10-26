package net.officefloor.hq.app.access;

import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;

/** Refuses a parent that is the sheet itself, a sheet never loaded, or one that has this sheet as an ancestor. */
public class RefuseBadParent {
    public void refuse(@Val SheetResponse sheet, @Val SettingInput input, SheetRepository sheets, RateRepository rates)
            throws BadRequestException {
        if (!Setting.PARENT.equals(input.key()) || input.value() == null) {
            return;
        }
        String parent = input.value();
        if (parent.equals(sheet.id())) {
            throw new BadRequestException("A sheet cannot be its own parent");
        }
        if (!sheets.exists(parent)) {
            throw new BadRequestException("No sheet " + parent + " has been loaded");
        }
        Set<String> seen = new HashSet<>();
        for (String at = parent; at != null && seen.add(at); at = rates.parentOf(at)) {
            if (at.equals(sheet.id())) {
                throw new BadRequestException("Sheet " + parent + " has this sheet as an ancestor");
            }
        }
    }
}
