import { useLayoutEffect, useRef } from 'react';
import type { ReactNode } from 'react';
import { showFigure } from './figures';
import type { Locale } from './localeQuery';

// Text never shown as a figure: what a person types or picks, and what is not shown at all.
const SKIP = new Set(['INPUT', 'TEXTAREA', 'SELECT', 'OPTION', 'SCRIPT', 'STYLE']);

/**
 * Shows every figure inside it under the user's locale. Figures are worked out and kept as text with a dot
 * (1234.56) everywhere — in the store, the queries, exports and the audit file — so the locale is applied only
 * here, where they are shown: any element whose whole text is a figure has it rewritten (1.234,56 under de),
 * and rewritten again whenever React changes it. Each rewritten text remembers what React wrote, so a figure
 * that stops being an element's whole text, or a change back to en, restores it.
 */
export function LocaleFigures({ locale, children }: { locale: Locale; children: ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);
  // text node -> what React wrote and what is shown instead
  const written = useRef(new WeakMap<Text, { original: string; shown: string }>());

  useLayoutEffect(() => {
    const root = ref.current;
    if (!root) return;

    const apply = (node: Text) => {
      const parent = node.parentElement;
      if (!parent || SKIP.has(parent.tagName)) return;
      const seen = written.current.get(node);
      const original = seen && node.data === seen.shown ? seen.original : node.data;
      const text = original.trim();
      const whole = parent.textContent?.trim() === node.data.trim();
      const shown = whole ? original.replace(text, showFigure(text, locale)) : original;
      if (shown !== original) written.current.set(node, { original, shown });
      else written.current.delete(node);
      if (node.data !== shown) node.data = shown;
    };
    const walk = (node: Node | null) => {
      if (!node) return;
      if (node.nodeType === Node.TEXT_NODE) {
        apply(node as Text);
        return;
      }
      const walker = document.createTreeWalker(node, NodeFilter.SHOW_TEXT);
      for (let t = walker.nextNode(); t; t = walker.nextNode()) apply(t as Text);
    };

    walk(root);
    const observer = new MutationObserver((mutations) => {
      // A text changed, added or removed changes whether its neighbours are their element's whole text.
      const touched = new Set<Node>();
      for (const m of mutations) {
        const at = m.type === 'characterData' ? m.target.parentNode : m.target;
        if (at && root.contains(at)) touched.add(at);
      }
      touched.forEach(walk);
      // Our own rewrites are observed too; drop them so they do not loop.
      observer.takeRecords();
    });
    observer.observe(root, { subtree: true, childList: true, characterData: true });
    return () => observer.disconnect();
  }, [locale]);

  return <div ref={ref}>{children}</div>;
}
