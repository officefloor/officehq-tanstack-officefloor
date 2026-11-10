import { PERSON_ROW_SPAN, PersonDetail } from '../../slots/defs/personRow';
import { useClaims, type Claim } from '../claims/useClaims';
import { usePeople, type Person } from '../people/usePeople';

/** The order of a timeline's kinds on one date. */
const KINDS = ['grade', 'claim', 'hold', 'retired', 'unretired'] as const;

type Entry = { date: string; kind: (typeof KINDS)[number]; text: string };

/** A person's timeline: a line per dated thing about them, by date, and on one date their grade changes, then
 *  claims, then holds, then retirements, then unretirements (alike lines in text order). A grade they were first
 *  given and a hold with no from-date carry no date and are left out. */
export function timeline(person: Person, claims: readonly Claim[]): string[] {
  const entries: Entry[] = [];
  for (const g of person.grades ?? []) {
    if (g.from !== null) entries.push({ date: g.from, kind: 'grade', text: `${g.from} grade ${g.grade}` });
  }
  for (const c of claims) {
    if (c.personId === person.id) entries.push({ date: c.date, kind: 'claim', text: `${c.date} claim ${c.amount} ${c.status}` });
  }
  for (const h of person.holds ?? []) {
    if (h.from !== null) entries.push({ date: h.from, kind: 'hold', text: `${h.from} hold${h.to !== null ? ` to ${h.to}` : ''}` });
  }
  for (const r of person.retirements ?? []) {
    entries.push({ date: r.from, kind: 'retired', text: `${r.from} retired` });
    entries.push({ date: r.to, kind: 'unretired', text: `${r.to} unretired` });
  }
  if (person.retiredFrom !== null) {
    entries.push({ date: person.retiredFrom, kind: 'retired', text: `${person.retiredFrom} retired` });
  }
  const cmp = (a: string, b: string) => (a < b ? -1 : a > b ? 1 : 0);
  entries.sort((a, b) => cmp(a.date, b.date) || KINDS.indexOf(a.kind) - KINDS.indexOf(b.kind) || cmp(a.text, b.text));
  return entries.map((e) => e.text);
}

function PersonTimeline({ sheetId, personId }: { sheetId: string; personId: number }) {
  const people = usePeople(sheetId);
  const claims = useClaims(sheetId);
  const p = people.data?.find((x) => x.id === personId);
  const lines = p ? timeline(p, claims.data ?? []) : [];

  return (
    <tr>
      <td colSpan={PERSON_ROW_SPAN}>
        {lines.length > 0 && <span>Timeline:</span>}
        {lines.map((line, i) => (
          <div key={i} data-testid="person-timeline-line">
            {line}
          </div>
        ))}
      </td>
    </tr>
  );
}

export const contributions = [PersonDetail.fill({ order: 30, Component: PersonTimeline })];
