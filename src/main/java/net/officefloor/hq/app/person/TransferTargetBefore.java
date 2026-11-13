package net.officefloor.hq.app.person;

import net.officefloor.hq.app.rollup.Rollup;

/** The other sheet's figures before a transfer, so its rollup can tell which of them the transfer moved. */
public record TransferTargetBefore(Rollup rollup) {
}
