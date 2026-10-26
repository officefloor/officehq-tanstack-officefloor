import { useState } from 'react';
import type { FormEvent } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useSheetId } from './useSheetId';
import { sheetKey } from './sheetQuery';

export function SheetPicker() {
  const [sheetId, setSheetId] = useSheetId();
  const queryClient = useQueryClient();
  const [typed, setTyped] = useState(sheetId);
  const [error, setError] = useState('');

  const submit = (e: FormEvent) => {
    e.preventDefault();
    const id = typed.trim();
    if (id === '') {
      setError('Type a sheet id to load.');
      return;
    }
    setError('');
    // Loading a sheet always re-reads it from the store, even when it was loaded before.
    void queryClient.invalidateQueries({ queryKey: sheetKey(id) });
    setSheetId(id);
  };

  return (
    <form data-testid="sheet-form" onSubmit={submit}>
      <label>
        Sheet{' '}
        <input
          data-testid="sheet-id-input"
          value={typed}
          onChange={(e) => {
            setTyped(e.target.value);
            setError('');
          }}
        />
      </label>{' '}
      <button type="submit" data-testid="sheet-load">
        Load
      </button>
      {error && <p data-testid="sheet-error">{error}</p>}
    </form>
  );
}
