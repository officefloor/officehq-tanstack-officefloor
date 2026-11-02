import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { useAging } from './useAging';

/** A person's due of their open claims that count, split by the claim's age on the sheet's today. */
function PersonAging({ sheetId, personId }: { sheetId: string; personId: number }) {
  const aging = useAging(sheetId).get(personId);
  return (
    <>
      <td data-testid="person-aged-0-30">{aging?.d0to30 ?? ''}</td>
      <td data-testid="person-aged-31-60">{aging?.d31to60 ?? ''}</td>
      <td data-testid="person-aged-61">{aging?.d61 ?? ''}</td>
    </>
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
];
