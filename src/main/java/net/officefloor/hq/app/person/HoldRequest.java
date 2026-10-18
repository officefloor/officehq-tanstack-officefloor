package net.officefloor.hq.app.person;

/** The body of holding or releasing a person: true puts them on hold, false releases them. */
public record HoldRequest(Boolean held) {
}
