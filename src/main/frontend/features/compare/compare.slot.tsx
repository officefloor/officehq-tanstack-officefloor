import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { getJsonLive } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser } from '../session/session';
import { GRADES } from '../people/people';
import { figuresOf } from '../consolidated/consolidated.slot';

// The sheet comparison: one line per grade of this sheet — `<grade> <this> <other>`: each the grade's due-total as
// that sheet's summary shows it (the people not retired on the grade in force on its own today, added up skipping
// n/a), 0.00 for a grade the other sheet has no one on. A sheet the store does not hold gives no lines (and is not
// started by the comparison). Both sheets are read as the screen shows them (as of its date, if any).

async function compareSheets(sheetId: string, other: string): Promise<string[]> {
  const id = other.trim();
  if (id === '' || !(await getJsonLive<boolean>(`/api/sheets/${encodeURIComponent(id)}/exists`))) return [];
  const [own, theirs] = await Promise.all([figuresOf(sheetId), figuresOf(id)]);
  return GRADES.map((g) => `${g} ${own.grades[g]} ${theirs.grades[g]}`);
}

function SheetCompare() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [other, setOther] = useState('');
  const compare = useMutation({ mutationFn: (id: string) => compareSheets(sheet ?? '', id) });
  const submit = useSubmitOnce(compare);
  if (!user || !sheet) return null;

  return (
    <div>
      <h2>Compare with another sheet</h2>
      <input data-testid="compare-sheet" placeholder="sheet id" value={other}
        onChange={(e) => { setOther(e.target.value); compare.reset(); }} />
      <button type="button" data-testid="compare" disabled={compare.isPending} onClick={() => submit(other)}>
        Compare
      </button>
      {compare.isSuccess && (
        <ul>
          {compare.data.map((line, i) => (
            <li key={i} data-testid="compare-line">{line}</li>
          ))}
        </ul>
      )}
      {compare.isError && <span data-testid="compare-error">The sheets could not be compared.</span>}
    </div>
  );
}

export const contribution = HomeMain.fill({ order: 19.4, Component: SheetCompare });
