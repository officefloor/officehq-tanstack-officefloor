import type { Claim } from '../claims/useClaims';
import { formatCents, toCents } from '../rates/due';
import type { Person } from './usePeople';

/** The person's timeline: one line per dated thing about them, by date, and on one date their grade changes, then
 *  their claims, then holds, then retirements, then unretirements, each kind by its line's text:
 *  "<date> grade <grade>", "<date> claim <amount> <status>", "<date> hold [to <date>]", "<date> retired",
 *  "<date> unretired". The grade from the beginning of time has no date and no line. */
export function timelineOf(person: Person, claims: Claim[]): string[] {
  const entries: { date: string; kind: number; seq: number; line: string }[] = [];
  const add = (date: string, kind: number, line: string) => entries.push({ date, kind, seq: entries.length, line: `${date} ${line}` });
  for (const g of person.grades) if (g.from !== '') add(g.from, 0, `grade ${g.grade}`);
  for (const c of claims.filter((c) => c.personId === person.id)) add(c.date, 1, `claim ${formatCents(toCents(c.amount))} ${c.status}`);
  for (const h of person.holds) add(h.from, 2, h.to === '' ? 'hold' : `hold to ${h.to}`);
  const unretired: string[] = [];
  for (const r of person.retirements ?? []) {
    const [from, to] = r.split(' to ');
    add(from, 3, 'retired');
    unretired.push(to);
  }
  if (person.retired && person.retiredFrom !== '') add(person.retiredFrom, 3, 'retired');
  for (const to of unretired) add(to, 4, 'unretired');
  entries.sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : a.kind - b.kind || (a.line < b.line ? -1 : a.line > b.line ? 1 : a.seq - b.seq)));
  return entries.map((e) => e.line);
}
