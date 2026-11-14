import type { Claim } from '../claims/claims';
import type { Person } from '../people/people';

// The kinds of dated thing on a person's timeline, in the order they are listed within one date.
const GRADE = 0;
const CLAIM = 1;
const HOLD = 2;
const RETIRED = 3;
const UNRETIRED = 4;

/**
 * The person's timeline: one line per dated thing about them, in date order — within a date their grade changes,
 * then claims (in order of their lines' text), then holds, then retirements, then unretirements. A grade or hold with
 * no from-date (from the beginning of time) has no date and is left out.
 */
export function timelineOf(person: Person, claims: Claim[]): string[] {
  const entries: { date: string; kind: number; seq: number; text: string }[] = [];
  const add = (date: string | null, kind: number, seq: number, text: string) => {
    if (date !== null) entries.push({ date, kind, seq, text: `${date} ${text}` });
  };
  person.grades.forEach((g, i) => add(g.from, GRADE, i, `grade ${g.grade}`));
  for (const c of claims) if (c.personId === person.id) add(c.date, CLAIM, 0, `claim ${c.amount} ${c.status}`);
  person.holds.forEach((h, i) => add(h.from, HOLD, i, h.to === null ? 'hold' : `hold to ${h.to}`));
  person.pastRetirements.forEach((r, i) => {
    add(r.from, RETIRED, i, 'retired');
    add(r.to, UNRETIRED, i, 'unretired');
  });
  add(person.retiredFrom, RETIRED, person.pastRetirements.length, 'retired');
  entries.sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : a.kind - b.kind || a.seq - b.seq || (a.text < b.text ? -1 : a.text > b.text ? 1 : 0)));
  return entries.map((e) => e.text);
}
