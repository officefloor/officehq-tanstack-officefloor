import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { PeopleHead, PersonRow } from '../../slots/defs/personRow';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { type Person } from '../people/people';
import { useGradeCodes } from '../grades/grades';
import { usePersonRowAction } from '../people/rowAction';

function InlineGradeHead() {
  return <th>Set grade</th>;
}

/** The row's grade select and save: records the chosen grade from the sheet's today exactly as the person form would
 *  (PERSON_EDITED and the figures it moved). The select shows the person's grade until another is chosen. A refused
 *  save shows the row's error and changes nothing. */
function PersonInlineGrade({ sheetId, user, person }: { sheetId: string; user: string; person: Person }) {
  const gradeCodes = useGradeCodes(sheetId);
  const queryClient = useQueryClient();
  const [chosen, setChosen] = useState<string | undefined>(undefined);
  const submit = useSubmitOnce();
  const rowAction = usePersonRowAction(sheetId, person.id, 'inline-grade');
  const save = useMutation({
    mutationFn: (grade: string) =>
      postJson<Person>(`/api/sheets/${encodeURIComponent(sheetId)}/people/${person.id}/grade`, { grade, by: user }),
    onSuccess: () => {
      setChosen(undefined);
      return queryClient.invalidateQueries({ queryKey: ['sheet', sheetId] });
    },
    onSettled: submit.end,
  });
  const grade = chosen ?? person.grade;

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
        {gradeCodes.map((g) => (
          <option key={g} value={g}>
            {g}
          </option>
        ))}
      </select>{' '}
      <button
        type="button"
        data-testid="person-inline-grade-save"
        disabled={save.isPending}
        onClick={() => {
          if (!submit.begin()) return;
          rowAction.take();
          save.reset();
          save.mutate(grade);
        }}
      >
        Save grade
      </button>
      {save.isError && rowAction.isLatest && <span data-testid="person-error"> Could not save this person's grade.</span>}
    </td>
  );
}

export const contributions = [
  PeopleHead.fill({ order: 3, Component: InlineGradeHead }),
  PersonRow.fill({ order: 3, Component: PersonInlineGrade }),
];
