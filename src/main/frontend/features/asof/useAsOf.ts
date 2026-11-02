import { useEffect, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { currentAsOfDate, setAsOfDate } from '../../api/http';
import { asString, useSearchParam } from '../../url/useSearchParam';

/** The date this screen shows the sheet as of (URL key `asOf`; '' when it shows the sheet as it is now).
 *  It belongs to this user's screen only, and is left as it is by loading a sheet. Every request is sent
 *  with it, so a change of it re-reads every query. */
export function useAsOf(): [string, (date: string | undefined) => void] {
  const [asOf, setAsOf] = useSearchParam('asOf', asString);
  const queryClient = useQueryClient();
  const changed = useRef(false);
  if (currentAsOfDate() !== asOf) {
    setAsOfDate(asOf);
    changed.current = true;
  }
  useEffect(() => {
    if (!changed.current) return;
    changed.current = false;
    void queryClient.resetQueries();
  }, [asOf, queryClient]);
  return [asOf, setAsOf];
}
