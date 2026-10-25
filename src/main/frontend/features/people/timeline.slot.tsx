import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useClaims, type Claim } from '../claims/claims';
import type { Person } from './people';

// A person's timeline: one line per dated thing about them, by date; on one date their grade changes come first,
// then their claims (by their line: amount, then status), then their holds, then their retirements, then their
// unretirements. Their first grade, which applies from the beginning of time, has no date and no line.
function TimelineHeader() {
  return <th>Timeline</th>;
}

type Line = { date: string; kind: number; text: string };

export function timelineOf(person: Person, claims: Claim[]): string[] {
  const lines: Line[] = [];
  for (const g of person.grades) {
    if (g.from !== null) lines.push({ date: g.from, kind: 0, text: `grade ${g.grade}` });
  }
  claims
    .filter((c) => c.personId === person.id)
    .map((c) => ({ date: c.date, kind: 1, text: `claim ${c.amount} ${c.status}` }))
    .sort((a, b) => (a.text < b.text ? -1 : a.text > b.text ? 1 : 0))
    .forEach((l) => lines.push(l));
  for (const h of person.holds) {
    lines.push({ date: h.from, kind: 2, text: h.to === null ? 'hold' : `hold to ${h.to}` });
  }
  for (const r of person.retirements) {
    lines.push({ date: r.from, kind: 3, text: 'retired' });
    lines.push({ date: r.to, kind: 4, text: 'unretired' });
  }
  if (person.retiredFrom !== null) lines.push({ date: person.retiredFrom, kind: 3, text: 'retired' });
  return lines
    .map((l, i) => ({ ...l, i }))
    .sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : a.kind - b.kind || a.i - b.i))
    .map((l) => `${l.date} ${l.text}`);
}

function PersonTimeline({ person }: { person: Person }) {
  const { data: claims } = useClaims();
  return (
    <td>
      {timelineOf(person, claims ?? []).map((line, i) => (
        <div key={i} data-testid="person-timeline-line">{line}</div>
      ))}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 51, Component: TimelineHeader }),
  PersonCells.fill({ order: 51, Component: PersonTimeline }),
];
