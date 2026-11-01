import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { useClaims } from '../claims/useClaims';
import type { Person } from '../people/usePeople';

/** On one date, which kind of line comes first: grade changes, then claims, holds, retirements, unretirements. */
const RANK = { grade: 0, claim: 1, hold: 2, retired: 3, unretired: 4 } as const;

type Line = { date: string; rank: number; text: string };

/**
 * A person's timeline: one line per dated thing about them, by date, then by kind (see RANK), then
 * as the lines read — `<date> claim <amount> <status>` for each of their claims (DELETED ones too),
 * `<date> grade <grade>` for each grade from a date (not the first, from the beginning of time),
 * `<date> hold [to <date>]` for each hold, `<date> retired` for each retirement (past or current) and
 * `<date> unretired` for each past retirement's end.
 */
function PersonTimeline({ sheet, person }: { user: string; sheet: string; person: Person }) {
  const { data: claims = [] } = useClaims(sheet);
  const lines: Line[] = [];
  const add = (date: string, kind: keyof typeof RANK, rest: string) =>
    lines.push({ date, rank: RANK[kind], text: `${date} ${kind}${rest}` });

  for (const g of person.grades) if (g.from !== null) add(g.from, 'grade', ` ${g.grade}`);
  for (const c of claims) if (c.personId === person.id) add(c.date, 'claim', ` ${c.amount} ${c.status}`);
  for (const h of person.holds) add(h.from, 'hold', h.to !== null ? ` to ${h.to}` : '');
  for (const r of person.pastRetirements) {
    add(r.from, 'retired', '');
    add(r.to, 'unretired', '');
  }
  if (person.retiredFrom !== null) add(person.retiredFrom, 'retired', '');

  lines.sort((a, b) =>
    a.date !== b.date ? (a.date < b.date ? -1 : 1) : a.rank !== b.rank ? a.rank - b.rank : a.text < b.text ? -1 : a.text > b.text ? 1 : 0,
  );

  return (
    <td>
      {lines.map((line, i) => (
        <div key={i} data-testid="person-timeline-line">
          {line.text}
        </div>
      ))}
    </td>
  );
}

export const contributions = [
  PersonColumns.fill({ order: 36.7, Component: () => <th>Timeline</th> }),
  PersonCells.fill({ order: 36.7, Component: PersonTimeline }),
];
