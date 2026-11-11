import { PersonHead, PersonRow } from '../../slots/defs/personRow';
import type { Person } from '../people/people';
import { useClaims } from '../claims/claims';
import type { Claim } from '../claims/claims';

function TimelineHead() {
  return <th>Timeline</th>;
}

/** The order of a date's lines: grade changes, then claims, then holds, then retirements, then unretirements. */
const KIND = { grade: 0, claim: 1, hold: 2, retired: 3, unretired: 4 } as const;

type Line = { date: string; kind: number; text: string };

/**
 * A person's timeline: one line per dated thing about them, by date, then by kind (see KIND), then
 * by the line itself — a grade they were given from a date, each of their claims (any status,
 * deleted too), each hold, each retirement (past and current) and each unretirement.
 */
export function timeline(person: Person, claims: Claim[]): string[] {
  const lines: Line[] = [];
  person.grades.forEach((g) => {
    if (g.from !== '') lines.push({ date: g.from, kind: KIND.grade, text: `grade ${g.grade}` });
  });
  claims
    .filter((c) => c.personId === person.id)
    .forEach((c) =>
      lines.push({ date: c.date, kind: KIND.claim, text: `claim ${c.amount} ${c.status}` }),
    );
  person.holds.forEach((h) =>
    lines.push({ date: h.from, kind: KIND.hold, text: h.to === '' ? 'hold' : `hold to ${h.to}` }),
  );
  person.retirements.forEach((r) => {
    const [from, to] = r.split(' to ');
    lines.push({ date: from, kind: KIND.retired, text: 'retired' });
    lines.push({ date: to, kind: KIND.unretired, text: 'unretired' });
  });
  if (person.retiredFrom !== '') {
    lines.push({ date: person.retiredFrom, kind: KIND.retired, text: 'retired' });
  }
  lines.sort((a, b) => a.date.localeCompare(b.date) || a.kind - b.kind || (a.text < b.text ? -1 : a.text > b.text ? 1 : 0));
  return lines.map((l) => `${l.date} ${l.text}`);
}

function PersonTimeline({ sheetId, person }: { sheetId: string; person: Person }) {
  const claims = useClaims(sheetId).data ?? [];
  return (
    <td>
      {timeline(person, claims).map((line, i) => (
        <div key={i} data-testid="person-timeline-line">
          {line}
        </div>
      ))}
    </td>
  );
}

export const contributions = [
  PersonHead.fill({ order: 45, Component: TimelineHead }),
  PersonRow.fill({ order: 45, Component: PersonTimeline }),
];
