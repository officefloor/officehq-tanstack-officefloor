import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { useIsLatest } from '../../query/useIsLatest';
import { byUser } from '../session/session';
import { PersonColumnHeaders, PersonColumns } from '../../slots/defs/personRow';
import { GRADES, peopleKey, peoplePath, personActionKey } from './people';
import type { Grade, Person } from './people';

function InlineGradeHeader() {
  return <th>Set grade</th>;
}

// The row's grade select (showing the person's grade until another is chosen) and the button that saves the chosen
// grade from the sheet's today, exactly as the person form would with its from-date blank.
function PersonInlineGrade({ sheet, user, person }: { sheet: string; user: string; person: Person }) {
  const queryClient = useQueryClient();
  const [chosen, setChosen] = useState<Grade | null>(null);
  const grade = chosen ?? person.grade;
  const save = useMutation({
    mutationKey: personActionKey(sheet, person.id),
    mutationFn: (body: { grade: Grade }) =>
      postJson<Person[]>(byUser(`${peoplePath(sheet)}/${person.id}/grade`, user), body),
    onSuccess: (updated) => {
      queryClient.setQueryData(peopleKey(sheet), updated);
      setChosen(null);
    },
  });
  const act = useSubmitOnce(save);
  const latest = useIsLatest(personActionKey(sheet, person.id), save.submittedAt);

  return (
    <td>
      <select
        data-testid="person-inline-grade"
        value={grade}
        onChange={(e) => {
          setChosen(e.target.value as Grade);
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
        disabled={save.isPending}
        onClick={() => act({ grade })}
      >
        Save grade
      </button>
      {save.isError && latest && (
        <span data-testid="person-error" role="alert">
          {' '}That grade was refused.
        </span>
      )}
    </td>
  );
}

export const contributions = [
  PersonColumnHeaders.fill({ order: 4, Component: InlineGradeHeader }),
  PersonColumns.fill({ order: 4, Component: PersonInlineGrade }),
];
