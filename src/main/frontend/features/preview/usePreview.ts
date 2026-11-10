import { useEffect, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { currentPreviewDate, setPreviewDate } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** The date this screen previews the sheet at (URL key `preview`; '' when it shows the sheet at its real today).
 *  It belongs to this user's screen only, and is left as it is by loading a sheet. Every read is sent with it,
 *  so a change of it re-reads every query. */
export function usePreview(): [string, (date: string | undefined) => void] {
  const [preview, setPreview] = useSearchParam('preview', asString);
  const queryClient = useQueryClient();
  const changed = useRef(false);
  if (currentPreviewDate() !== preview) {
    setPreviewDate(preview);
    changed.current = true;
  }
  useEffect(() => {
    if (!changed.current) return;
    changed.current = false;
    void queryClient.resetQueries();
  }, [preview, queryClient]);
  return [preview, setPreview];
}
