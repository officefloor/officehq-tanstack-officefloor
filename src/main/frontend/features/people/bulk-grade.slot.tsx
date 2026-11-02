import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { byUser } from '../session/session';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { peopleKey, peoplePath } from './people';
import { useGradeCodes } from '../grades/grades';
import type { Grade, Person } from './people';
import { useGradeCodes } from '../grades/grades';
import { clearSelectedPeople, selectPerson, useSelectedPeople } from './selection';

type BulkGraded = { graded: number; people: Person[] };

function SelectHeader() {
  return <th>Select</th>;
}

// The tick that selects the person for a bulk action on people.
function PersonSelect({ sheet, person }: { sheet: string; user: string; person: Person }) {
  const selected = useSelectedPeople(sheet);
  return (
    <td>
      <input
        type="checkbox"
        data-testid="person-select"
        checked={selected.has(person.id)}
        onChange={(e) => selectPerson(sheet, person.id, e.target.checked)}
      />
    </td>
  );
}

// Gives the selected people a grade together, from a from-date (blank for the sheet's today): the server saves each
// in list order exactly as the person form would, and refuses the whole action on a bad date. Shows how many changed.
function BulkGrade({ sheet, user }: { sheet: string; user: string }) {
  const gradeCodes = useGradeCodes(sheet);
  const queryClient = useQueryClient();
  const selected = useSelectedPeople(sheet);
  const [grade, setGrade] = useState<Grade>('A');
  const [from, setFrom] = useState('');
  const apply = useMutation({
    mutationFn: (body: { people: number[]; grade: Grade; from: string }) =>
      postJson<BulkGraded>(byUser(`${peoplePath(sheet)}/bulk-grade`, user), body),
    onSuccess: (result) => {
      queryClient.setQueryData(peopleKey(sheet), result.people);
      clearSelectedPeople(sheet);
    },
  });
  const act = useSubmitOnce(apply);

  return (
    <p>
      Selected people: {selected.size}{' '}
      <label>
        Grade{' '}
        <select
          data-testid="bulk-grade"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value as Grade);
            apply.reset();
          }}
        >
          {gradeCodes.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>
      </label>{' '}
      <label>
        From{' '}
        <input
          data-testid="bulk-grade-from"
          placeholder="yyyy-mm-dd"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            apply.reset();
          }}
        />
      </label>{' '}
      <button
        type="button"
        data-testid="bulk-grade-apply"
        disabled={apply.isPending}
        onClick={() => act({ people: [...selected], grade, from: from.trim() })}
      >
        Grade selected
      </button>
      {apply.isSuccess && (
        <>
          {' '}Changed <strong data-testid="bulk-graded">{apply.data.graded}</strong>
        </>
      )}
      {apply.isError && (
        <span data-testid="person-error" role="alert">
          {' '}That bulk grade was refused: a grade of A, B or C and a from-date that is a valid date or blank.
        </span>
      )}
    </p>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 1, Component: SelectHeader }),
  PersonColumns.fill({ order: 1, Component: PersonSelect }),
  SheetPanels.fill({ order: 12, Component: BulkGrade }),
];
