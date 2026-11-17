import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { useClaims, type Claim } from '../claims/claims';
import { timeline } from './timeline';

function TimelineHead() {
  return <th>Timeline</th>;
}

/** A person's timeline: one line per dated thing about them, in date order (see timeline). */
function PersonTimeline({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = useClaims(sheetId);
  const mine = (claims.data ?? []).filter((c: Claim) => c.personId === person.id);
  return (
    <td>
      {timeline(person, mine).map((line, i) => (
        <div key={i} data-testid="person-timeline-line">
          {line}
        </div>
      ))}
    </td>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 96, Component: TimelineHead }),
  PersonRow.fill({ order: 96, Component: PersonTimeline }),
];
