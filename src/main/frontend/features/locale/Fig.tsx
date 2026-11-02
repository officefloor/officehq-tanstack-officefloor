import { useCurrentSheet, useCurrentUser } from '../session/session';
import { showFigure, useLocale } from './locale';

/** A cell's text as the signed-in user's locale shows it (see {@link showFigure}). Wrap every figure the screen
 *  shows in it; never an input's value, an export or anything sent to the server. */
export function Fig({ children }: { children: string | number | null | undefined }) {
  const [sheet] = useCurrentSheet();
  const [user] = useCurrentUser();
  const { data } = useLocale(sheet, user);
  return <>{showFigure(children == null ? '' : String(children), data?.locale)}</>;
}
