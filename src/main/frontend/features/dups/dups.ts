import type { Claim } from '../claims/claims';
import type { Person } from '../people/people';

/** One set of duplicate claims: a person's claims of one date and amount, how many. */
export type Dup = { name: string; date: string; amount: string; count: number };

/**
 * The duplicate claims: for each person, date and amount with two or more claims (VOID and
 * DELETED ones left out), people in list order, then by date, then by amount (as a number).
 */
export function duplicates(people: Person[], claims: Claim[]): Dup[] {
  const out: Dup[] = [];
  for (const p of people) {
    const groups = new Map<string, Dup>();
    for (const c of claims) {
      if (c.personId !== p.id || c.status === 'VOID' || c.status === 'DELETED') continue;
      const key = `${c.date} ${c.amount}`;
      const g = groups.get(key);
      if (g) g.count++;
      else groups.set(key, { name: p.name, date: c.date, amount: c.amount, count: 1 });
    }
    const dups = [...groups.values()].filter((g) => g.count >= 2);
    dups.sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : Number(a.amount) - Number(b.amount)));
    out.push(...dups);
  }
  return out;
}
