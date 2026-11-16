package net.officefloor.hq.app.rollup;

import java.util.Map;

/** Every person's figures on the sheet just before a change, by person id in the order they were added. */
public record RollupBefore(Map<Long, Figures> figures) {
}
