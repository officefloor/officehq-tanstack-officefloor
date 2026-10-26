import { useQuery } from '@tanstack/react-query';
import { sheetQuery } from './sheetQuery';

/** The sheet's today (yyyy-mm-dd), or '' until loaded. Every default date uses this, never `new Date()`. */
export function useSheetToday(sheetId: string): string {
  return useQuery(sheetQuery(sheetId)).data?.today ?? '';
}
