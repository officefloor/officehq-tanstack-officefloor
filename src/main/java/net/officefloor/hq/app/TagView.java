package net.officefloor.hq.app;

/**
 * What the API exposes for a tag: the shape the front-end renders as a chip (and as an option in the
 * add-tag select). Just the id and the label.
 */
public record TagView(Long id, String name) {

    public static TagView of(Tag tag) {
        return new TagView(tag.getId(), tag.getName());
    }
}
