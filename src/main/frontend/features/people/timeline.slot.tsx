import { PersonColumnHeads, PersonColumns } from '../../slots/defs/personColumns';
import { type Person } from './people';
import { useCurrentSheet } from '../../session/session';
import { useClaims } from '../claims/claims';

// A person's timeline: one line per dated thing about them, in date order. On one date their grade changes
// come first, then their claims (by their line, as text), then holds, then retirements, then unretirements.
// Their first grade (from the beginning of time) is no change, so it has no line.
function TimelineHead() {
  return <th>Timeline</th>;
}

type Entry = { date: string; kind: number; seq: number; text: string };

const cmp = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);

function PersonTimeline({ person }: { person: Person }) {
  const [sheet] = useCurrentSheet();
  const claims = useClaims(sheet);
  const entries: Entry[] = [];
  person.grades.forEach((g, i) => {
    if (g.from !== null) entries.push({ date: g.from, kind: 0, seq: i, text: `grade ${g.grade}` });
  });
  (claims.data ?? [])
    .filter((c) => c.personId === person.id)
    .forEach((c) => entries.push({ date: c.date, kind: 1, seq: 0, text: `claim ${c.amount} ${c.status}` }));
  person.holds.forEach((h, i) =>
    entries.push({ date: h.from, kind: 2, seq: i, text: h.to === null ? 'hold' : `hold to ${h.to}` }),
  );
  person.pastRetirements.forEach((r, i) => {
    entries.push({ date: r.from, kind: 3, seq: i, text: 'retired' });
    entries.push({ date: r.to, kind: 4, seq: i, text: 'unretired' });
  });
  if (person.retiredFrom !== null) {
    entries.push({ date: person.retiredFrom, kind: 3, seq: person.pastRetirements.length, text: 'retired' });
  }
  entries.sort((a, b) => cmp(a.date, b.date) || a.kind - b.kind || a.seq - b.seq || cmp(a.text, b.text));
  return (
    <td>
      {entries.map((e, i) => (
        <div key={i} data-testid="person-timeline-line">
          {e.date} {e.text}
        </div>
      ))}
    </td>
  );
}

export const contributions = [
  PersonColumnHeads.fill({ order: 200, Component: TimelineHead }),
  PersonColumns.fill({ order: 200, Component: PersonTimeline }),
];
