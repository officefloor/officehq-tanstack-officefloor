import { useState } from 'react';
import { SheetPanels } from '../../slots/defs/sheetPanels';
import { usePreview } from './usePreview';

/** Previews the sheet on this user's screen as if its today were a date (or as it is, when cleared),
 *  and shows the date previewed. */
function PreviewPicker() {
  const [preview, setPreview] = usePreview();
  const [date, setDate] = useState('');

  return (
    <form
      data-testid="preview-form"
      onSubmit={(e) => {
        e.preventDefault();
        const trimmed = date.trim();
        if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) setPreview(trimmed);
      }}
    >
      Preview: <strong data-testid="sheet-preview">{preview}</strong>{' '}
      <label>
        Preview as if today were{' '}
        <input
          data-testid="preview-date"
          placeholder="yyyy-mm-dd"
          value={date}
          onChange={(e) => setDate(e.target.value)}
        />
      </label>{' '}
      <button type="submit" data-testid="preview-set">
        Set
      </button>{' '}
      <button type="button" data-testid="preview-clear" onClick={() => setPreview(undefined)}>
        Clear
      </button>
    </form>
  );
}

export const contribution = SheetPanels.fill({ order: 1, Component: PreviewPicker });
