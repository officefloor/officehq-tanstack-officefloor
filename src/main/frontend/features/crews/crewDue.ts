import { sumDues } from '../rates/useRates';
import type { Crew } from './useCrews';

/** A crew's due-total: the lead's and the members' due-totals added up, 'n/a' (or no figure) skipped. */
export function crewDue(crew: Crew, dueTotals: Map<number, string>): string {
  return sumDues([crew.leadId, ...crew.members.map((m) => m.personId)]
    .map((id) => dueTotals.get(id))
    .filter((d): d is string => d !== undefined));
}
