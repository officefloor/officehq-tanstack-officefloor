import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { HttpError, postJson } from '../../api/http';
import { useSubmitOnce } from '../../query/useSubmitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { sheetKey } from '../sheet/sheet';
import { GRADES } from '../people/people';
import { type Grade, type GradeMerged, gradesKey, useGrades } from './grades';

// The sheet's grades, whether each is retired, and the select a grade is retired with. A grade already
// retired is refused. A steward merges a grade into another: the server moves everyone on the merged grade
// to the other from the sheet's today, as the person form would, and the merge done shows once it answers
// (gone while it runs). The same grade twice, or anyone but a steward, is refused.
function Grades() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const grades = useGrades(sheet);
  const [grade, setGrade] = useState<string>(GRADES[0]);
  const queryClient = useQueryClient();

  const retire = useMutation({
    mutationFn: (g: string) =>
      postJson<Grade>(`/api/sheets/${encodeURIComponent(sheet)}/grades/retire`, { grade: g, user }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: gradesKey(sheet) });
    },
  });
  const submit = useSubmitOnce(retire);

  const [from, setFrom] = useState<string>(GRADES[0]);
  const [into, setInto] = useState<string>(GRADES[1]);
  const merge = useMutation({
    mutationFn: (p: { from: string; into: string }) =>
      postJson<GradeMerged>(`/api/sheets/${encodeURIComponent(sheet)}/grades/merge`, { ...p, user }),
    onSuccess: async () => {
      // A merge moves people's grades, figures and their claims' dues, so the whole sheet is re-read.
      await queryClient.invalidateQueries({ queryKey: sheetKey(sheet) });
    },
  });
  const submitMerge = useSubmitOnce(merge);
  const failed = retire.isError ? retire.error : merge.isError ? merge.error : null;

  return (
    <section data-testid="grades">
      <h2>Grades</h2>
      <table>
        <thead>
          <tr>
            <th>Grade</th>
            <th>Retired</th>
          </tr>
        </thead>
        <tbody>
          {(grades.data ?? []).map((g) => (
            <tr key={g.grade} data-testid={`grade-row-${g.grade}`}>
              <td data-testid="grade-code">{g.grade}</td>
              <td data-testid="grade-retired">{g.retired ? 'yes' : 'no'}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <p>
        <select
          data-testid="grade-retire-select"
          aria-label="Grade to retire"
          value={grade}
          onChange={(e) => {
            setGrade(e.target.value);
            retire.reset();
            merge.reset();
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
          data-testid="grade-retire"
          disabled={retire.isPending}
          onClick={() => {
            merge.reset();
            submit(grade);
          }}
        >
          Retire grade
        </button>
      </p>
      <p>
        <select
          data-testid="grade-merge-from"
          aria-label="Grade to merge"
          value={from}
          onChange={(e) => {
            setFrom(e.target.value);
            merge.reset();
            retire.reset();
          }}
        >
          {GRADES.map((g) => (
            <option key={g} value={g}>
              {g}
            </option>
          ))}
        </select>{' '}
        into{' '}
        <select
          data-testid="grade-merge-into"
          aria-label="Grade to merge into"
          value={into}
          onChange={(e) => {
            setInto(e.target.value);
            merge.reset();
            retire.reset();
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
          data-testid="grade-merge"
          disabled={merge.isPending}
          onClick={() => {
            retire.reset();
            submitMerge({ from, into });
          }}
        >
          Merge grade
        </button>{' '}
        <span data-testid="grade-merged">
          {merge.isSuccess ? `${merge.data.from} into ${merge.data.into}: ${merge.data.merged} moved` : ''}
        </span>
      </p>
      {failed && (
        <p data-testid="grade-error" role="alert">
          {failed instanceof HttpError && failed.reason ? `${failed.reason}.` : 'The grade was refused.'}
        </p>
      )}
    </section>
  );
}

export const contribution = HomeMain.fill({ order: 40.2, Component: Grades });
