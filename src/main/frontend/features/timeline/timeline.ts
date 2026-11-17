import type { Person } from '../people/people';
import type { Claim } from '../claims/claims';

/** The order of a date's lines by kind: grade changes, then claims, then holds, then retirements, then unretirements. */
const KINDS = ['grade', 'claim', 'hold', 'retired', 'unretired'] as const;
type Entry = { date: string; kind: (typeof KINDS)[number]; text: string };

/** A person's timeline lines, in date order (a date's lines by kind, then as text): `<date> grade <grade>` for each
 *  dated grade that changes the one before it; `<date> claim <amount> <status>` for each of their claims (any status);
 *  `<date> hold [to <date>]` for each hold; `<date> retired` for each retirement, past or current; `<date> unretired`
 *  for each past retirement's end. */
export function timeline(person: Person, claims: Claim[]): string[] {
  const entries: Entry[] = [];
  const grades = [...(person.grades ?? [])].sort((a, b) =>
    a.from === b.from ? 0 : a.from === null ? -1 : b.from === null ? 1 : a.from.localeCompare(b.from),
  );
  let before: string | null = null;
  for (const g of grades) {
    if (g.from !== null && g.grade !== before) entries.push({ date: g.from, kind: 'grade', text: `grade ${g.grade}` });
    before = g.grade;
  }
  for (const c of claims) entries.push({ date: c.date, kind: 'claim', text: `claim ${c.amount} ${c.status}` });
  for (const h of person.holds ?? [])
    entries.push({ date: h.from, kind: 'hold', text: h.to === null ? 'hold' : `hold to ${h.to}` });
  for (const r of person.retirements ?? []) {
    entries.push({ date: r.from, kind: 'retired', text: 'retired' });
    entries.push({ date: r.to, kind: 'unretired', text: 'unretired' });
  }
  if (person.retiredFrom !== null) entries.push({ date: person.retiredFrom, kind: 'retired', text: 'retired' });
  entries.sort(
    (a, b) =>
      a.date.localeCompare(b.date) ||
      KINDS.indexOf(a.kind) - KINDS.indexOf(b.kind) ||
      (a.text < b.text ? -1 : a.text > b.text ? 1 : 0),
  );
  return entries.map((e) => `${e.date} ${e.text}`);
}
