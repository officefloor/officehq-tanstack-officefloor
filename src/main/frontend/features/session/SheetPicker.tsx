import { useState } from 'react';
import type { FormEvent } from 'react';
import { useCurrentSheet } from './session';

// Choose the active sheet by typing its id and loading it.
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
      {sheet && (
        <span>
          {' '}Sheet: <strong data-testid="sheet-current-id">{sheet}</strong>
        </span>
      )}
    </form>
  );
}
