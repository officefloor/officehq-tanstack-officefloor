package net.officefloor.hq.app.voidreason;

import java.util.List;

/** The void reasons on a sheet, in the order they were added. */
public record VoidReasonsResponse(List<VoidReasonResponse> reasons) {
}
