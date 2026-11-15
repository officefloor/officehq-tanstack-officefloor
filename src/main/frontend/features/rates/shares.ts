import { liveClaims, type Claim } from '../claims/api';
import { gradeOn, type Person } from '../people/api';
import type { DatedOn } from '../settings/api';
import { claimDue, heldParts, type HeldPart, type Late, type Rate } from './api';

/** The parts a person holds of the sheet's live claims (`claims`: all of them; `people`: everyone), each by the
 *  claim's due as its own person's row shows it. */
export function heldOf(personId: number, claims: Claim[], people: Person[] | undefined, rates: Rate[] | undefined,
  floor: DatedOn, late: Late): HeldPart[] {
  const byId = new Map((people ?? []).map((p) => [p.id, p]));
  return heldParts(personId, liveClaims(claims, people), (c) => {
    const owner = byId.get(c.personId);
    return owner ? claimDue(c, rates, (d) => gradeOn(owner, d), floor, late) : null;
  });
}
