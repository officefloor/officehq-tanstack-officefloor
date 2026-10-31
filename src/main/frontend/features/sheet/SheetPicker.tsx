import { useState } from 'react';
import { useCurrentSheet } from '../session/useSession';

/** Choose the active sheet by typing its id and loading it. */
export function SheetPicker() {
  const [sheet, setSheet] = useCurrentSheet();
  const [id, setId] = useState('');

  return (
    <form
      data-testid="sheet-form"
      onSubmit={(e) => {
        e.preventDefault();
        const trimmed = id.trim();
        if (trimmed !== '') setSheet(trimmed);
      }}
    >
      <label>
        Sheet id{' '}
        <input data-testid="sheet-id-input" value={id} onChange={(e) => setId(e.target.value)} />
      </label>{' '}
      <button type="submit" data-testid="sheet-load">
        Load
      </button>{' '}
      {sheet !== '' && (
        <span>
          Sheet: <strong data-testid="sheet-current-id">{sheet}</strong>
        </span>
      )}
    </form>
  );
}
