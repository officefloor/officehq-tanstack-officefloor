import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { getJsonLive } from '../../api/http';
import { useSubmitOnce } from '../../api/submitOnce';
import { HomeMain } from '../../slots/defs/homeMain';
import { useCurrentSheet, useCurrentUser, type SheetInfo } from '../session/session';
import { claimsPath, type Claim } from '../claims/claims';
import { personDue } from '../rates/due.slot';
import { ratesPath, type Rate } from '../rates/rates';
import { settingsPath, type Settings } from '../roles/roles';
import { peoplePath, shownName, type Person } from './people';
import { poolsPath, type Pool } from '../pools/pools';

// The tree search: over the sheet and every other sheet of its tree (its ancestors and descendants), every person —
// retired ones too — whose name or an alias contains the typed text, case ignored (blank: everyone), one line each:
// the sheet id, their name with the grade in force on their sheet's today and their due-total as their sheet shows it. The sheet's own people come first in list
// order, then the other sheets' by sheet id. Read from the store as it is now; the count shows once done, and is
// gone while it runs.

const treePath = (sheetId: string) => `/api/sheets/${encodeURIComponent(sheetId)}/tree`;

/** The lines for one sheet's matching people, in list order. */
async function linesOf(sheetId: string, text: string): Promise<string[]> {
  const [info, people, claims, rates, settings, pools] = await Promise.all([
    getJsonLive<SheetInfo>(`/api/sheets/${encodeURIComponent(sheetId)}`),
    getJsonLive<Person[]>(peoplePath(sheetId)),
    getJsonLive<Claim[]>(claimsPath(sheetId)),
    getJsonLive<Rate[]>(ratesPath(sheetId)),
    getJsonLive<Settings>(settingsPath(sheetId)),
    getJsonLive<Pool[]>(poolsPath(sheetId)),
  ]);
  const key = text.toLowerCase();
  return people
    .filter((p) => [p.name, ...p.aliases].some((n) => n.toLowerCase().includes(key)))
    .map((p) => `${sheetId} ${shownName(p, info.today)} ${personDue(p, claims, rates, info.today, settings, people, pools)}`);
}

async function searchTree(sheetId: string, text: string): Promise<string[]> {
  const others = await getJsonLive<string[]>(treePath(sheetId));
  return (await Promise.all([sheetId, ...others].map((id) => linesOf(id, text)))).flat();
}

function TreeSearch() {
  const [user] = useCurrentUser();
  const [sheet] = useCurrentSheet();
  const [text, setText] = useState('');
  const search = useMutation({ mutationFn: (typed: string) => searchTree(sheet ?? '', typed) });
  const submit = useSubmitOnce(search);
  if (!user || !sheet) return null;

  return (
    <div>
      <h2>Search the tree</h2>
      <input data-testid="search-name" value={text} onChange={(e) => setText(e.target.value)} />
      <button type="button" data-testid="search" disabled={search.isPending} onClick={() => submit(text)}>
        Search
      </button>
      {search.isSuccess && (
        <>
          {' '}Found <span data-testid="search-count">{search.data.length}</span>
          <ul>
            {search.data.map((line, i) => (
              <li key={i} data-testid="search-line" style={{ whiteSpace: 'pre' }}>{line}</li>
            ))}
          </ul>
        </>
      )}
      {search.isError && <span data-testid="search-error">The tree could not be searched.</span>}
    </div>
  );
}

export const contribution = HomeMain.fill({ order: 19.2, Component: TreeSearch });
