import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { GRADES } from '../people/people';
import { gradesPath, gradesQueryKey, useGrades, type Grade } from './grades';

// The grades, each showing whether it is retired, and the select and button that retire one. A grade already
// retired is refused by the server and shows the error, which goes as soon as the select changes.
function Grades() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const grades = useGrades();
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const queryClient = useQueryClient();
  const retire = useMutation({
    mutationFn: (body: { grade: string }) => postJson<Grade[]>(`${gradesPath(sheet ?? '')}/retire`, body, user),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: gradesQueryKey(sheet ?? '') });
    },
  });
  const submit = useSubmitOnce(retire);

  if (!user || !sheet) return null;
  return (
    <section data-testid="grades">
      <h2>Grades</h2>
      <ul>
        {(grades.data ?? []).map((g) => (
          <li key={g.grade} data-testid={`grade-row-${g.grade}`}>
            {g.grade} — retired: <span data-testid="grade-retired">{g.retired ? 'yes' : 'no'}</span>
          </li>
        ))}
      </ul>
      <select data-testid="grade-retire-select" value={grade}
        onChange={(e) => { setGrade(e.target.value); retire.reset(); }}>
        {GRADES.map((g) => (
          <option key={g} value={g}>{g}</option>
        ))}
      </select>{' '}
      <button type="button" data-testid="grade-retire" disabled={retire.isPending} onClick={() => submit({ grade })}>
        Retire grade
      </button>
      {retire.isError && <span data-testid="grade-error">The grade could not be retired.</span>}
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 6.55, Component: Grades });
