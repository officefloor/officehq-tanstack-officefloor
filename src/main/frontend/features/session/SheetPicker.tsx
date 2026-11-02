import { useState } from 'react';
import type { FormEvent } from 'react';
import { useRouterState } from '@tanstack/react-router';
import { useCurrentSheet, useCurrentUser } from './session';
import { usePeople } from '../people/people';
import { useClaims } from '../claims/claims';
import { useUserRole } from '../roles/roles';
import { useCrews } from '../crews/crews';

// Choose the active sheet by typing its id and loading it. The sheet shows as loaded once its load (the route's
// re-read) is done and what its rows need is on hand (its people and claims, the user's role and the crews, which decide
// who a restricted user sees), so what is on screen then is the sheet as the store holds it.
export function SheetPicker() {
  const [sheet, setSheet] = useCurrentSheet();
  const [draft, setDraft] = useState('');

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = draft.trim();
    if (trimmed) setSheet(trimmed);
  };

  return (
    <form onSubmit={submit}>
      <label>
        Sheet <input data-testid="sheet-id-input" value={draft} onChange={(e) => setDraft(e.target.value)} />
      </label>
      <button type="submit" data-testid="sheet-load">
        Load
      </button>
      {sheet && <CurrentSheet sheet={sheet} />}
    </form>
  );
}

// The loaded sheet's id, shown once its load is done (see SheetPicker).
function CurrentSheet({ sheet }: { sheet: string }) {
  const loading = useRouterState({ select: (s) => s.status === 'pending' });
  const { data: people } = usePeople(sheet);
  const { data: claims } = useClaims(sheet);
  const [user] = useCurrentUser();
  const { data: role } = useUserRole(sheet, user ?? '');
  const { data: crews } = useCrews(sheet);
  if (loading || people === undefined || claims === undefined || role === undefined || crews === undefined) return null;
  return (
    <span>
      {' '}Sheet: <strong data-testid="sheet-current-id">{sheet}</strong>
    </span>
  );
}
