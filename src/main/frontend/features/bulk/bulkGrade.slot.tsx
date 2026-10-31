import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumns } from '../../slots/defs/personRow';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { claimsKey } from '../claims/useClaims';
import { GRADES, peopleKey, type Person } from '../people/usePeople';
import { useSheetToday } from '../sheet/useSheet';
import { useSelectedPeople } from './useSelectedPeople';

/** The tick that selects a person for a bulk action. */
function PersonSelect({ person }: { user: string; sheet: string; person: Person }) {
  const [selected, setSelected] = useSelectedPeople();
  return (
    <td>
      <input
        type="checkbox"
        data-testid="person-select"
        aria-label="Select person"
        checked={selected.includes(person.id)}
        onChange={(e) =>
          setSelected(e.target.checked ? [...selected, person.id] : selected.filter((id) => id !== person.id))
        }
      />
    </td>
  );
}

/**
 * Give the ticked people a grade together, from a from-date (blank: the sheet's today). Each, in
 * list order, gets it as saving the person form would. Shows how many were changed; the count goes
 * while the next press runs.
 */
function BulkGrade({ user, sheet }: { user: string; sheet: string }) {
  const queryClient = useQueryClient();
  const today = useSheetToday(sheet);
  const [selected, setSelected] = useSelectedPeople();
  const [grade, setGrade] = useState<string>('A');
  const [from, setFrom] = useState('');
  const bulk = useMutation({
    mutationFn: (personIds: number[]) =>
      postJson<{ graded: number }>(`/api/sheets/${encodeURIComponent(sheet)}/bulk-grade`, {
        personIds,
        grade,
        from: from.trim(),
        user,
      }),
    onSuccess: () => setSelected([]),
    onSettled: () =>
      Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheet) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheet) }),
      ]),
  });
  return (
    <section data-testid="bulk-grade-panel">
      <label>
        Grade{' '}
        <select
          data-testid="bulk-grade"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            bulk.reset();
          }}
        >
          {GRADES.map((g) => (
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
          placeholder={today || 'yyyy-mm-dd'}
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            bulk.reset();
          }}
        />
      </label>{' '}
      <button
        type="button"
        data-testid="bulk-grade-apply"
        disabled={bulk.isPending}
        onClick={() => {
          bulk.reset();
          bulk.mutate(selected);
        }}
      >
        Grade selected ({selected.length})
      </button>
      {bulk.isSuccess && (
        <span>
          {' '}
          Changed <strong data-testid="bulk-graded">{bulk.data.graded}</strong>
        </span>
      )}
      {bulk.isError && (
        <span data-testid="person-error" role="alert">
          {' '}
          {selected.length === 0 ? 'Select the people to grade.' : 'Could not grade the selected people.'}
        </span>
      )}
    </section>
  );
}

export const contributions = [
  PersonColumns.fill({ order: 1, Component: () => <th>Select</th> }),
  PersonCells.fill({ order: 1, Component: PersonSelect }),
  SheetPanels.fill({ order: 17, Component: BulkGrade }),
];
