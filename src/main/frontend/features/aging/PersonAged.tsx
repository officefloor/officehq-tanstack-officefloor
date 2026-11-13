import { useClaims } from '../claims/claims';
import { liveHolds } from '../people/people';
import type { Person } from '../people/people';
import { useRates } from '../rates/rates';
import { useDueRules } from '../settings/settings';
import { personAged } from './aging';
import type { AgeBucket } from './aging';

function usePersonAged(person: Person) {
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const claims = (useClaims().data ?? []).filter((c) => c.personId === person.id);
  return personAged(claims, rates, person.grades, liveHolds(person, rules.today), rules);
}

// The dues of a person's open counting claims aged 0 to 30, 31 to 60, or 61 or more days on the sheet's today.
const bucketCell = (bucket: AgeBucket) =>
  function PersonAgedCell({ person }: { person: Person }) {
    return <td data-testid={`person-aged-${bucket}`}>{usePersonAged(person)[bucket]}</td>;
  };

export const PersonAged0to30 = bucketCell('0-30');
export const PersonAged31to60 = bucketCell('31-60');
export const PersonAged61 = bucketCell('61');
