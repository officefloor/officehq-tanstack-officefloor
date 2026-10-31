import { useState } from 'react';
import { useSearchParam, asString } from '../../url/useSearchParam';
import { PREVIEW_PARAM } from './usePreview';

// Sets or clears the signed-in user's preview date, and shows it (nothing when none). While it is set every figure on
// the screen is shown as if the sheet's today were that date; changes still use the real today. It is this screen's alone.
export function PreviewControl() {
  const [preview, setPreview] = useSearchParam(PREVIEW_PARAM, asString);
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
      Preview: <span data-testid="sheet-preview">{preview}</span>{' '}
      <input type="date" data-testid="preview-date" value={date} onChange={(e) => setDate(e.target.value)} />
      <button type="submit" data-testid="preview-set">Set preview</button>
      <button type="button" data-testid="preview-clear" onClick={() => setPreview(undefined)}>
        Clear preview
      </button>
    </form>
  );
}
