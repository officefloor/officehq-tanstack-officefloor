import type { ReactNode } from 'react';
import { formatFigure, useLocale } from './locale';

/** Shows a figure (an amount, due figure, total, rate, share or cut) in the signed-in user's locale.
 *  Wrap the content of every element that shows one; text that is not a figure is shown as it is. */
export function Fig({ children }: { children: ReactNode }) {
  const locale = useLocale();
  if (typeof children === 'string') return <>{formatFigure(children, locale)}</>;
  if (typeof children === 'number') return <>{formatFigure(String(children), locale)}</>;
  return <>{children}</>;
}
