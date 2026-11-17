/** A person's score from their settled figures (two decimals, see personSplit) and their overdue count: the settled
 *  figures / 100, minus 5 for every overdue claim, rounded once, half to even, to a whole number. */
export function score(settled: string, overdueCount: number): string {
  const [whole, frac = ''] = settled.replace('-', '').split('.');
  const h = BigInt(whole) * 100n + BigInt(frac.padEnd(2, '0').slice(0, 2));
  const p = (settled.startsWith('-') ? -h : h) - 50000n * BigInt(overdueCount); // in 1/10000 of the score
  const a = p < 0n ? -p : p;
  const q = a / 10000n;
  const rem = a % 10000n;
  const r = rem > 5000n || (rem === 5000n && q % 2n === 1n) ? q + 1n : q;
  return String(p < 0n ? -r : r);
}
