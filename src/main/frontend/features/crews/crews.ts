import { useQuery } from '@tanstack/react-query';
import { getJson } from '../../api/http';
import { sheetKey, useCurrentSheet } from '../session/session';
import { useClaims } from '../claims/claims';
import { liveHolds, usePeople } from '../people/people';
import { useDueRules } from '../settings/settings';
import { NA, personDue, total, useRates } from '../rates/rates';

/** A crew's lead or member: a person of the sheet. */
export type CrewPerson = { id: number; name: string };

/** A crew on the sheet: its name, its lead (null: none, once its lead was transferred away) and its members in the order they were added. */
export type Crew = { id: number; name: string; lead: CrewPerson | null; members: CrewPerson[] };

/** The query key for the sheet's crews; under sheetKey, so re-loading the sheet re-reads them. */
export const crewsKey = (sheet: string) => [...sheetKey(sheet), 'crews'] as const;

/** The sheet's crews, in the order they were added. */
export function useCrews() {
  const [sheet] = useCurrentSheet();
  return useQuery({
    queryKey: crewsKey(sheet),
    queryFn: () => getJson<Crew[]>(`/api/sheets/${encodeURIComponent(sheet)}/crews`),
    enabled: !!sheet,
  });
}

/** Each person's due-total by id, as the people list shows it (n/a when there is none). */
export function useDueByPerson(): (personId: number) => string {
  const people = usePeople().data ?? [];
  const rates = useRates().data ?? [];
  const rules = useDueRules();
  const claims = useClaims().data ?? [];
  return (personId) => {
    const person = people.find((p) => p.id === personId);
    if (!person) return NA;
    const own = claims.filter((c) => c.personId === personId);
    return personDue(own, rates, person.grades, liveHolds(person, rules.today), rules, { personId, claims, people });
  };
}

/** A crew's due-total: its lead's and members' due-totals added up, skipping n/a. */
export function crewDue(crew: Crew, dueOf: (personId: number) => string): string {
  return total([...(crew.lead ? [crew.lead] : []), ...crew.members].map((p) => dueOf(p.id)));
}
