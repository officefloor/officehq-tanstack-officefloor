package net.officefloor.hq.app.delegation;

import java.util.List;

/** The delegations made on a sheet, in the order they were made. */
public record DelegationsResponse(List<DelegationResponse> delegations) {
}
