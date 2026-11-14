import type { Claim } from '../claims/claims';

/** A duplicate: a person's claims on one date for one amount, counted. */
export type Dup = { name: string; date: string; amount: string; count: number };

/**
 * The duplicate claims: for each person, date and amount with two or more claims (VOID and DELETED ones left out),
 * people in list order, then by date, then by amount.
 */
export function duplicates(people: { id: number; name: string }[], claims: Claim[]): Dup[] {
  const out: Dup[] = [];
  for (const p of people) {
    const groups = new Map<string, Dup>();
    for (const c of claims) {
      if (c.personId !== p.id || c.status === 'VOID' || c.status === 'DELETED') continue;
      const key = `${c.date}|${c.amount}`;
      const g = groups.get(key) ?? { name: p.name, date: c.date, amount: c.amount, count: 0 };
      g.count++;
      groups.set(key, g);
    }
    const dups = [...groups.values()].filter((g) => g.count >= 2);
    dups.sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : Number(a.amount) - Number(b.amount)));
    out.push(...dups);
  }
  return out;
}
