import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useSheetId } from '../session/session';
import { sheetKey } from './api';

/** Choose a sheet by typing its id and loading it. */
export function SheetPicker() {
  const queryClient = useQueryClient();
  const [, setSheetId] = useSheetId();
  const [id, setId] = useState('');
  const [error, setError] = useState(false);

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        const trimmed = id.trim();
        if (!trimmed) {
          setError(true);
          return;
        }
        // Loading a sheet re-reads it from the store, even when it was on screen before.
        queryClient.removeQueries({ queryKey: sheetKey(trimmed) });
        setSheetId(trimmed);
      }}
    >
      <label>
        Sheet{' '}
        <input
          data-testid="sheet-id-input"
          value={id}
          onChange={(e) => {
            setId(e.target.value);
            setError(false);
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="sheet-load">
        Load
      </button>
      {error && <span data-testid="sheet-load-error">Type a sheet id to load.</span>}
    </form>
  );
}
