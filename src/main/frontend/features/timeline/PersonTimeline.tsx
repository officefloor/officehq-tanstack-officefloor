import { useClaims } from '../claims/claims';
import type { Person } from '../people/people';
import { timelineOf } from './timeline';

// The person's timeline: one line per dated thing about them, in date order (see timelineOf).
export function PersonTimeline({ person }: { person: Person }) {
  const lines = timelineOf(person, useClaims().data ?? []);
  return (
    <td>
      <ol>
        {lines.map((line, i) => (
          <li key={i} data-testid="person-timeline-line">
            {line}
          </li>
        ))}
      </ol>
    </td>
  );
}
