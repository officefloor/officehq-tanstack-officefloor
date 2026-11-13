package net.officefloor.hq.app.alias;

/** A person and one of their aliases, as the screen reads them. */
public record AliasResponse(long personId, String alias) {

    public static AliasResponse from(Alias alias) {
        return new AliasResponse(alias.personId(), alias.alias());
    }
}
