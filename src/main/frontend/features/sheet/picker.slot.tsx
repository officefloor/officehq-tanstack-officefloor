import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { AppHeader } from '../../slots/defs/appHeader';
import { useCurrentSheet, useCurrentUser } from '../../session/session';
import { sheetKey, useSheet } from './sheet';

function SheetPicker() {
  const [user] = useCurrentUser();
  const [sheet, setSheet] = useCurrentSheet();
  const [input, setInput] = useState('');
  const queryClient = useQueryClient();
  const loaded = useSheet(sheet);

  if (!user) return null;
  return (
    <form
      data-testid="sheet-form"
      onSubmit={(e) => {
        e.preventDefault();
        const id = input.trim();
        if (!id) return;
        // Loading an id again re-reads it from the store.
        void queryClient.invalidateQueries({ queryKey: sheetKey(id) });
        setSheet(id);
      }}
    >
      <input data-testid="sheet-id-input" placeholder="Sheet id" value={input} onChange={(e) => setInput(e.target.value)} />
      <button type="submit" data-testid="sheet-load">
        Load
      </button>
      {loaded.data && (
        <span>
          {' '}Sheet <strong data-testid="sheet-current-id">{loaded.data.id}</strong>
        </span>
      )}
    </form>
  );
}

export const contribution = AppHeader.fill({ order: 10, Component: SheetPicker });
