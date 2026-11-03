import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PersonCells, PersonColumnHead } from '../../slots/defs/personRow';
import { claimsKey } from '../claims/useClaims';
import { GRADES, peopleKey, usePeople, type Person } from '../people/usePeople';
import { usePersonRowAction } from '../people/usePersonRowAction';

/** A grade select on the person's row (showing their grade until another is chosen) and a button that
 *  records the chosen grade from the sheet's today, exactly as the person form would. */
function PersonInlineGrade({ sheetId, personId }: { sheetId: string; personId: number }) {
  const people = usePeople(sheetId);
  const queryClient = useQueryClient();
  const p = people.data?.find((x) => x.id === personId);
  const [chosen, setChosen] = useState<string | null>(null);
  const grade = chosen ?? p?.grade ?? 'A';

  const save = useMutation({
    mutationFn: (body: { grade: string }) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${personId}/grade`, body),
    onSuccess: () => setChosen(null),
    onSettled: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: peopleKey(sheetId) }),
        queryClient.invalidateQueries({ queryKey: claimsKey(sheetId) }),
      ]);
    },
  });
  const announce = usePersonRowAction(sheetId, personId, save.reset);

  return (
    <td>
      <select
        data-testid="person-inline-grade"
        value={grade}
        onChange={(e) => {
          setChosen(e.target.value);
          save.reset();
        }}
      >
        {GRADES.map((g) => (
          <option key={g} value={g}>
            {g}
          </option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="person-inline-grade-save"
        disabled={save.isPending || !p}
        onClick={() => {
          announce();
          save.reset();
          save.mutate({ grade });
        }}
      >
        Save grade
      </button>
      {save.isError && <span data-testid="person-error">{save.error.message}</span>}
    </td>
  );
}

export const contributions = [
  PersonColumnHead.fill({ order: 2.5, Component: () => <th>Set grade</th> }),
  PersonCells.fill({ order: 2.5, Component: PersonInlineGrade }),
];
