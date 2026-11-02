import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import type { Person } from './people';
import { useClaims, type Claim } from '../claims/claims';

function TimelineHeader() {
  return <th>Timeline</th>;
}

// The kinds of line, in the order they are listed within one date.
const GRADE = 0;
const CLAIM = 1;
const HOLD = 2;
const RETIRED = 3;
const UNRETIRED = 4;

type Line = { date: string; kind: number; text: string };

/** The person's timeline: one line per dated thing about them (a grade from a from-date — the one from the beginning
 *  of time has none —, a claim of theirs, whatever its status, a hold, a retirement past or current, an unretirement),
 *  by date, and within a date grades, then claims, then holds, then retirements, then unretirements, each kind by its
 *  line's text. */
export function timeline(person: Person, claims: Claim[]): string[] {
  const lines: Line[] = [];
  for (const g of person.grades) {
    if (g.from !== null) lines.push({ date: g.from, kind: GRADE, text: `grade ${g.grade}` });
  }
  for (const c of claims.filter((c) => c.person === person.id)) {
    lines.push({ date: c.date, kind: CLAIM, text: `claim ${c.amount} ${c.status}` });
  }
  for (const h of person.holds) {
    lines.push({ date: h.from, kind: HOLD, text: h.to === null ? 'hold' : `hold to ${h.to}` });
  }
  for (const r of person.retirements) {
    lines.push({ date: r.from, kind: RETIRED, text: 'retired' });
    lines.push({ date: r.to, kind: UNRETIRED, text: 'unretired' });
  }
  if (person.retiredFrom !== null) lines.push({ date: person.retiredFrom, kind: RETIRED, text: 'retired' });
  return lines
    .sort((a, b) => a.date.localeCompare(b.date) || a.kind - b.kind || (a.text < b.text ? -1 : a.text > b.text ? 1 : 0))
    .map((l) => `${l.date} ${l.text}`);
}

function PersonTimeline({ sheet, person }: { sheet: string; user: string; person: Person }) {
  const { data: claims = [] } = useClaims(sheet);
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
  PersonColumnHeaders.fill({ order: 85, Component: TimelineHeader }),
  PersonColumns.fill({ order: 85, Component: PersonTimeline }),
];
