import { gradeOn, usePeople } from '../people/usePeople';
import { inForce, useSettings } from '../settings/useSettings';

/** A claim's grace days: those of its person's grade in force on its date, as in force on that date (0 for none). */
export type Grace = (claim: { personId?: string; date: string }) => number;

export function useGrace(sheet: string): Grace {
  const settings = useSettings(sheet);
  const people = usePeople(sheet);
  const byId = new Map(people.map((p) => [p.id, p]));
  return (claim) => {
    const person = claim.personId === undefined ? undefined : byId.get(claim.personId);
    if (person === undefined) return 0;
    const days = inForce(settings, `grace-${gradeOn(person, claim.date)}`, claim.date, '');
    return days === '' ? 0 : Number.parseInt(days, 10);
  };
}
