import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimDueCents, isOpen, live, useClaims, type Claim } from '../claims/useClaims';
import { gradeOn, heldOn, usePeople, type Person } from '../people/usePeople';
import { claimRateOn, showDue, useRates } from '../rates/useRates';
import { useFloor, useLate } from '../settings/useSettings';
import { useSheetToday } from '../sheet/useSheet';

/** Whole days from a claim's date to the sheet's today; negative for a claim dated after today. */
function ageOn(claim: Claim, today: string): number {
  return Math.round((Date.parse(`${today}T00:00:00Z`) - Date.parse(`${claim.date}T00:00:00Z`)) / 86_400_000);
}

/** A person's due on their OPEN and PENDING claims that count (as in their due-total: not deferred,
 *  not retired out unless a statement binds them), split by the claim's age on the sheet's today: 0 to 30
 *  days (a claim dated after today too), 31 to 60, and 61 or more. Not capped. */
function useAgedCents(sheet: string): (person: Person) => bigint[] {
  const { data: all = [] } = useClaims(sheet);
  const today = useSheetToday(sheet);
  const { data: rates = [] } = useRates(sheet);
  const floor = useFloor(sheet);
  const lateBy = useLate(sheet);
  return (person) => {
    const claims = live(
      all.filter((c) => c.personId === person.id),
      (_, date) => heldOn(person, date, today),
    ).filter(isOpen);
    const buckets = [0n, 0n, 0n];
    for (const c of claims) {
      const due = claimDueCents(c, floor, claimRateOn(rates, gradeOn(person, c.date), c), lateBy);
      if (due === null) continue;
      const age = ageOn(c, today);
      buckets[age <= 30 ? 0 : age <= 60 ? 1 : 2] += due;
    }
    return buckets;
  };
}

function PersonAged({ sheet, person }: { user: string; sheet: string; person: Person }) {
  const buckets = useAgedCents(sheet)(person);
  return (
    <>
      <td data-testid="person-aged-0-30">{showDue(buckets[0])}</td>
      <td data-testid="person-aged-31-60">{showDue(buckets[1])}</td>
      <td data-testid="person-aged-61">{showDue(buckets[2])}</td>
    </>
  );
}

/** The sheet's age buckets: everyone's age buckets added up, not trimmed. */
function SheetAged({ sheet }: { user: string; sheet: string }) {
  const { data: people = [] } = usePeople(sheet);
  const agedOf = useAgedCents(sheet);
  const buckets = [0n, 0n, 0n];
  for (const p of people) agedOf(p).forEach((cents, i) => (buckets[i] += cents));
  return (
    <p>
      Due by age: 0–30 days <strong data-testid="rollup-aged-0-30">{showDue(buckets[0])}</strong>, 31–60 days{' '}
      <strong data-testid="rollup-aged-31-60">{showDue(buckets[1])}</strong>, 61+ days{' '}
      <strong data-testid="rollup-aged-61">{showDue(buckets[2])}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumns.fill({
    order: 11,
    Component: () => (
      <>
        <th>Due 0–30 days</th>
        <th>Due 31–60 days</th>
        <th>Due 61+ days</th>
      </>
    ),
  }),
  PersonCells.fill({ order: 11, Component: PersonAged }),
  SheetPanels.fill({ order: 20.6, Component: SheetAged }),
];
