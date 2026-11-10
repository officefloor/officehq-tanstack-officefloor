package net.officefloor.hq.app.sheet;

import java.util.List;

/** A sheet's descendants: its children, theirs, and so on, each once, nearest first. */
public record DescendantsResponse(List<String> descendants) {
}
