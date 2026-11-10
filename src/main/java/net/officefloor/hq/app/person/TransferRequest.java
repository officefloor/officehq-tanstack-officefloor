package net.officefloor.hq.app.person;

/** A transfer as the screen sends it: the id of the sheet to transfer the person to. */
public record TransferRequest(String toSheet) {
}
