package net.officefloor.hq.app.sheet;

import java.util.List;

/** The sheet's descendants (its children, their children and so on), nearest first. */
public record DescendantsResponse(List<String> sheets) {
}
