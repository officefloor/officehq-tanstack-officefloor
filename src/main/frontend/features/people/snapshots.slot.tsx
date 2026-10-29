import { useQuery } from '@tanstack/react-query';
import { PersonCells, PersonHeaders } from '../../slots/defs/personColumns';
import { peopleQuery } from './peopleQuery';

/** The person's snapshot lines (see `Person.snapshots`): one per SNAPSHOT written for them, oldest first. */
function PersonSnapshots({ sheetId, personId }: { sheetId: string; personId: number }) {
  const person = (useQuery(peopleQuery(sheetId)).data ?? []).find((p) => p.id === personId);
  return (
    <td>
      {(person?.snapshots ?? []).map((s, i) => (
        <div key={i} data-testid="person-snapshot-line">
          {s.day} {s.dueTotal}
        </div>
      ))}
    </td>
  );
}

export const contributions = [
  PersonHeaders.fill({ order: 25, Component: () => <th>Snapshots</th> }),
  PersonCells.fill({ order: 25, Component: PersonSnapshots }),
];
