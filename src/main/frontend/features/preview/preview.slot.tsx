import { useState } from 'react';
import { SheetBody } from '../../slots/defs/sheetBody';
import { usePreview } from './usePreview';

/** Preview the sheet at a date: every figure on this screen is then worked out as if the sheet's today were that
 *  date, over the sheet's records as they are now, until it is cleared. Every change still uses the sheet's real
 *  today, and nothing a change of today does happens. */
function PreviewControl() {
  const [preview, setPreview] = usePreview();
  const [date, setDate] = useState('');
  return (
    <form
      data-testid="preview-form"
      onSubmit={(e) => {
        e.preventDefault();
        const want = date.trim();
        if (/^\d{4}-\d{2}-\d{2}$/.test(want)) setPreview(want);
      }}
    >
      Preview: <strong data-testid="sheet-preview">{preview}</strong>{' '}
      <label>
        Preview at{' '}
        <input
          type="text"
          placeholder="yyyy-mm-dd"
          data-testid="preview-date"
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

export const contribution = SheetBody.fill({ order: 1, Component: () => <PreviewControl /> });
