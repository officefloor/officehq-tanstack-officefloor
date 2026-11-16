import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { SheetPage } from '../../slots/defs/sheetPage';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { GRADES, type Person } from '../people/people';
import { useSelectedPeople } from './selection';

type Result = { graded: number };

function SelectHead() {
  return <th>Select</th>;
}

/** The person's select checkbox: the selection a bulk action on people takes. */
function PersonSelect({ person }: { sheetId: string; user: string; person: Person }) {
  const [selected, setSelected] = useSelectedPeople();
  return (
    <td>
      <input
        type="checkbox"
        data-testid="person-select"
        checked={selected.includes(person.id)}
        onChange={(e) =>
          setSelected(e.target.checked ? [...selected, person.id] : selected.filter((id) => id !== person.id))
        }
      />
    </td>
  );
}

/** Gives the selected people a grade together, from a date (blank: the sheet's today): each, in list order, is edited
 *  exactly as the person form would. Shows how many were changed; the count is cleared while a new press runs. A refused
 *  bulk grade shows the error and changes nothing. */
function BulkGrade({ sheetId, user }: { sheetId: string; user: string }) {
  const queryClient = useQueryClient();
  const [selected, setSelected] = useSelectedPeople();
  const [grade, setGrade] = useState<string>('A');
  const [from, setFrom] = useState('');
  const submit = useSubmitOnce();
  const apply = useMutation({
    mutationFn: (personIds: number[]) =>
      postJson<Result>(`/api/sheets/${encodeURIComponent(sheetId)}/bulk-grade`, {
        personIds,
        grade,
        from: from.trim(),
        by: user,
      }),
    onSuccess: async () => {
      setSelected([]);
      setFrom('');
      await queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onSettled: submit.end,
  });

  return (
    <p data-testid="bulk-grade-bar">
      Grade selected people{' '}
      <select
        data-testid="bulk-grade"
        value={grade}
        onChange={(e) => {
          setGrade(e.target.value);
          apply.reset();
        }}
      >
        {GRADES.map((g) => (
          <option key={g} value={g}>
            {g}
          </option>
        ))}
      </select>{' '}
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
        onClick={() => {
          if (!submit.begin()) return;
          apply.reset();
          apply.mutate(selected);
        }}
      >
        Give grade ({selected.length})
      </button>
      {apply.isSuccess && (
        <>
          {' '}
          Changed <span data-testid="bulk-graded">{apply.data.graded}</span>
        </>
      )}
      {apply.isError && <span data-testid="person-error"> Could not give the selected people the grade.</span>}
    </p>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 1, Component: SelectHead }),
  PersonRow.fill({ order: 1, Component: PersonSelect }),
  SheetPage.fill({ order: 11, Component: BulkGrade }),
];
