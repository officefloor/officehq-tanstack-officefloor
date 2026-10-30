import { useQuery } from '@tanstack/react-query';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { claimsQuery } from '../claims/claimsQuery';
import { peopleQuery } from './peopleQuery';

/** Within a date: grade changes, then claims, then holds, then retirements, then unretirements. */
const KIND_ORDER = { grade: 0, claim: 1, hold: 2, retired: 3, unretired: 4 } as const;

type Entry = { date: string; kind: keyof typeof KIND_ORDER; text: string };

/**
 * The person's timeline: one line per dated thing about them, by date — each grade they were given from a date
 * (not the first), each of their claims (within a date, by the line's text), each hold, each retirement (past and
 * current) and each unretirement.
 */
function PersonTimeline({ sheetId, personId }: { sheetId: string; personId: number }) {
  const person = (useQuery(peopleQuery(sheetId)).data ?? []).find((p) => p.id === personId);
  const claims = useQuery(claimsQuery(sheetId)).data ?? [];
  const entries: Entry[] = [];
  if (person) {
    person.grades.forEach((g) => {
      if (g.from !== null) entries.push({ date: g.from, kind: 'grade', text: `grade ${g.grade}` });
    });
    claims
      .filter((c) => c.personId === personId)
      .forEach((c) => entries.push({ date: c.date, kind: 'claim', text: `claim ${c.amount} ${c.status}` }));
    person.holds.forEach((h) =>
      entries.push({ date: h.from, kind: 'hold', text: h.to === null ? 'hold' : `hold to ${h.to}` }),
    );
    person.retirements.forEach((r) => {
      entries.push({ date: r.from, kind: 'retired', text: 'retired' });
      entries.push({ date: r.to, kind: 'unretired', text: 'unretired' });
    });
    if (person.retiredFrom) {
      entries.push({ date: person.retiredFrom, kind: 'retired', text: 'retired' });
    }
  }
  entries.sort(
    (a, b) => a.date.localeCompare(b.date) || KIND_ORDER[a.kind] - KIND_ORDER[b.kind] || (a.text < b.text ? -1 : a.text > b.text ? 1 : 0),
  );
  return (
    <td data-testid="person-timeline">
      {entries.map((e, i) => (
        <div key={i} data-testid="person-timeline-line">
          {e.date} {e.text}
        </div>
      ))}
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 25.5, Component: () => <th>Timeline</th> }),
  PersonCells.fill({ order: 25.5, Component: PersonTimeline }),
];
