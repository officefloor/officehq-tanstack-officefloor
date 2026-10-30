import { useState } from 'react';
import { asString, useSearchParam } from '../../url/useSearchParam';

// Chooses the active sheet by id; the active id lives in the URL (`sheet`). Everything else in the
// app is kept per sheet and reads this key. A never-used id is simply an empty sheet.
export function SheetPicker() {
  const [sheet, setSheet] = useSearchParam('sheet', asString);
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
      <input data-testid="sheet-id-input" value={id} onChange={(e) => setId(e.target.value)} placeholder="Sheet id" />
      <button type="submit" data-testid="sheet-load">Load</button>{' '}
      Sheet: <span data-testid="sheet-current-id">{sheet}</span>
    </form>
  );
}
