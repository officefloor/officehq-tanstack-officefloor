import { liveClaims, type Claim } from '../claims/api';
import { gradeOn, type Person } from '../people/api';
import type { DatedOn } from '../settings/api';
import { claimDue, heldParts, rateOn, type HeldPart, type Late, type Rate } from './api';

/** The parts a person holds of the sheet's live claims (`claims`: all of them; `people`: everyone), and those others
 *  hold of theirs (see `heldParts`): a live claim's by the rate of the holder's own grade in force on its date, a
 *  settled one's frozen with the due its own person's row shows. */
export function heldOf(personId: number, claims: Claim[], people: Person[] | undefined, rates: Rate[] | undefined,
  floor: DatedOn, late: Late): HeldPart[] {
  const byId = new Map((people ?? []).map((p) => [p.id, p]));
  return heldParts(personId, liveClaims(claims, people), (c) => {
    const owner = byId.get(c.personId);
    return owner ? claimDue(c, rates, (d) => gradeOn(owner, d), floor, late) : null;
  }, (c, s) => {
    const holder = byId.get(s.personId);
    return holder ? rateOn(rates, gradeOn(holder, c.date), c.date, c.id) : undefined;
  }, late);
}
