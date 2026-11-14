import { useClaims } from '../claims/claims';
import { liveHolds, usePeople } from '../people/people';
import { total, useRates } from '../rates/rates';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { useDueRules } from '../settings/settings';
import { personAged } from './aging';
import type { AgeBucket } from './aging';

// The sheet's dues per age bucket: everyone's buckets added up (not trimmed).
export function AgedTotals() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const people = usePeople().data ?? [];
  const claims = useClaims().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();

  if (!user || !sheet) return null;

  const each = people.map((p) =>
    personAged(claims.filter((c) => c.personId === p.id), rates, p.grades, liveHolds(p, rules.today), rules),
  );
  const sum = (bucket: AgeBucket) => total(each.map((a) => a[bucket]));
  return (
    <p>
      Aged: 0–30 days <strong data-testid="rollup-aged-0-30">{sum('0-30')}</strong>, 31–60 days{' '}
      <strong data-testid="rollup-aged-31-60">{sum('31-60')}</strong>, 61+ days{' '}
      <strong data-testid="rollup-aged-61">{sum('61')}</strong>
    </p>
  );
}
