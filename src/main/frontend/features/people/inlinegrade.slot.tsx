import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { putJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { useRowAction } from '../../api/rowAction';
import { PeopleColumns, PersonCells } from '../../slots/defs/peopleColumns';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { claimsQueryKey } from '../claims/claims';
import { peoplePath, peopleQueryKey, type Person } from './people';
import { useGradeCodes } from '../grades/grades';

// A grade select and save button on each person's row: gives them the chosen grade from the sheet's today. It is
// saved exactly as the person form would save it (same name, terms, contact and tags, the version the row was
// loaded with), so the server writes the same PERSON_EDITED and rollup lines, and refuses it the same way.
function InlineGradeHeader() {
  return <th>Regrade</th>;
}

function PersonInlineGrade({ person }: { person: Person }) {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [grade, setGrade] = useState(person.grade);
  const queryClient = useQueryClient();
  const gradeCodes = useGradeCodes();
  const save = useMutation({
    mutationFn: (body: { grade: string }) =>
      putJson<Person>(`${peoplePath(sheet ?? '')}/${person.id}`, {
        name: person.name,
        grade: body.grade,
        gradeFrom: '',
        terms: person.terms == null ? '' : String(person.terms),
        contact: person.contact ?? '',
        tags: (person.tags ?? []).join(', '),
        version: person.version,
      }, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') });
      await queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet ?? '') });
    },
  });
  const submit = useSubmitOnce(save);
  const rowAction = useRowAction(`person-${person.id}`);
  return (
    <td>
      <select data-testid="person-inline-grade" value={grade} onChange={(e) => { setGrade(e.target.value); save.reset(); }}>
        {gradeCodes.map((g) => (
          <option key={g} value={g}>{g}</option>
        ))}
      </select>{' '}
      <button type="button" data-testid="person-inline-grade-save" disabled={save.isPending}
        onClick={() => { rowAction.take(); submit({ grade }); }}>
        Save grade
      </button>
      {save.isError && rowAction.isLatest && <span data-testid="person-error">The grade could not be saved.</span>}
    </td>
  );
}

export const contributions = [
  PeopleColumns.fill({ order: 4.5, Component: InlineGradeHeader }),
  PersonCells.fill({ order: 4.5, Component: PersonInlineGrade }),
];
