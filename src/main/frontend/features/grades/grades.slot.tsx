import { useEffect, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postJson } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { GRADES, peopleQueryKey } from '../people/people';
import { claimsQueryKey } from '../claims/claims';
import { gradesPath, gradesQueryKey, useGrades, type Grade } from './grades';

// The grades, each showing whether it is retired, and the select and button that retire one. A grade already
// retired is refused by the server and shows the error, which goes as soon as the select changes.
// Below them, two selects and a button merge a grade into another (a steward only): the server gives everyone on the
// merged grade the other from the sheet's today, and the count of people changed shows once done (gone while it runs, and once a sheet is loaded).
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
  const [from, setFrom] = useState<string>(GRADES[0]);
  const [into, setInto] = useState<string>(GRADES[1]);
  const merge = useMutation({
    mutationFn: (body: { from: string; into: string }) =>
      postJson<{ merged: number }>(`${gradesPath(sheet ?? '')}/merge`, body, user),
    onSettled: async () => {
      await queryClient.invalidateQueries({ queryKey: peopleQueryKey(sheet ?? '') });
      await queryClient.invalidateQueries({ queryKey: claimsQueryKey(sheet ?? '') });
    },
  });
  const submitMerge = useSubmitOnce(merge);
  // A merge's count and error belong to the sheet it was made on: loading a sheet clears them.
  const resetMerge = merge.reset;
  useEffect(() => resetMerge(), [sheet, resetMerge]);

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
        onChange={(e) => { setGrade(e.target.value); retire.reset(); merge.reset(); }}>
        {GRADES.map((g) => (
          <option key={g} value={g}>{g}</option>
        ))}
      </select>{' '}
      <button type="button" data-testid="grade-retire" disabled={retire.isPending} onClick={() => { merge.reset(); submit({ grade }); }}>
        Retire grade
      </button>
      <div>
        Merge{' '}
        <select data-testid="grade-merge-from" value={from}
          onChange={(e) => { setFrom(e.target.value); merge.reset(); retire.reset(); }}>
          {GRADES.map((g) => (
            <option key={g} value={g}>{g}</option>
          ))}
        </select>{' '}into{' '}
        <select data-testid="grade-merge-into" value={into}
          onChange={(e) => { setInto(e.target.value); merge.reset(); retire.reset(); }}>
          {GRADES.map((g) => (
            <option key={g} value={g}>{g}</option>
          ))}
        </select>{' '}
        <button type="button" data-testid="grade-merge" disabled={merge.isPending}
          onClick={() => { retire.reset(); submitMerge({ from, into }); }}>
          Merge grade
        </button>
        {merge.isSuccess && <>{' '}Changed <span data-testid="grade-merged">{merge.data.merged}</span></>}
      </div>
      {retire.isError && <span data-testid="grade-error">The grade could not be retired.</span>}
      {merge.isError && <span data-testid="grade-error">The grade could not be merged.</span>}
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 6.55, Component: Grades });
