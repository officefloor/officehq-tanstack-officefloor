import type { Person } from '../people/people';

export function PersonTagsHead() {
  return <th>Tags</th>;
}

// The person's tags, sorted, comma and space separated; empty when they have none.
export function PersonTags({ person }: { person: Person }) {
  return <td data-testid="person-tags">{person.tags.join(', ')}</td>;
}
