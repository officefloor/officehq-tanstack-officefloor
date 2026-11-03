import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { SheetBody } from '../../slots/defs/sheetBody';
import { sumDues } from '../rates/useRates';
import { useAging } from './useAging';
import { useFigure } from '../locale/useLocale';

/** A person's due of their open claims that count, split by the claim's age on the sheet's today. */
function PersonAging({ sheetId, personId }: { sheetId: string; personId: number }) {
  const fig = useFigure();
  const aging = useAging(sheetId).get(personId);
  return (
    <>
      <td data-testid="person-aged-0-30">{fig(aging?.d0to30 ?? '')}</td>
      <td data-testid="person-aged-31-60">{fig(aging?.d31to60 ?? '')}</td>
      <td data-testid="person-aged-61">{fig(aging?.d61 ?? '')}</td>
    </>
  );
}

/** The sheet's aging: everyone's age buckets simply added up (not trimmed). */
function SheetAging({ sheetId }: { sheetId: string }) {
  const fig = useFigure();
  const aging = [...useAging(sheetId).values()];
  return (
    <p>
      Aged 0–30: <strong data-testid="rollup-aged-0-30">{fig(sumDues(aging.map((a) => a.d0to30)))}</strong>,
      31–60 <strong data-testid="rollup-aged-31-60">{fig(sumDues(aging.map((a) => a.d31to60)))}</strong>,
      61+ <strong data-testid="rollup-aged-61">{fig(sumDues(aging.map((a) => a.d61)))}</strong>
    </p>
  );
}

export const contributions = [
  PersonColumnHead.fill({ order: 23, Component: () => (
    <>
      <th>Aged 0–30</th>
      <th>Aged 31–60</th>
      <th>Aged 61+</th>
    </>
  ) }),
  PersonCells.fill({ order: 23, Component: PersonAging }),
  SheetBody.fill({ order: 33, Component: ({ sheetId }) => <SheetAging sheetId={sheetId} /> }),
];
