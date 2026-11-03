import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { usePeople } from '../people/usePeople';

/** A person's snapshot lines: one per SNAPSHOT written for them (each change of the sheet's today), oldest
 *  first, as the day being left and their due-total as it stood then. */
function PersonSnapshots({ sheetId, personId }: { sheetId: string; personId: number }) {
  const people = usePeople(sheetId);
  const p = people.data?.find((x) => x.id === personId);
  return (
    <td>
      {(p?.snapshots ?? []).map((s, i) => (
        <div key={i} data-testid="person-snapshot-line">
          {s.day ?? ''} {s.dueTotal}
        </div>
      ))}
    </td>
  );
}

export const contributions = [
  PersonColumnHead.fill({ order: 57, Component: () => <th>Snapshots</th> }),
  PersonCells.fill({ order: 57, Component: PersonSnapshots }),
];
