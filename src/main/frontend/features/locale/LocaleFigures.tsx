import { useEffect, useRef } from 'react';
import type { ReactNode } from 'react';
import { formatFigure } from './locale';
import type { Locale } from './locale';

// Text inside these is never a figure shown in a cell (option labels are matched by tests and people as typed).
const SKIP = new Set(['OPTION', 'TEXTAREA', 'SCRIPT', 'STYLE']);

/** Shows every figure inside it in the user's locale: any element whose whole text is a number with two decimals
 *  (an amount, due, total, rate, share, cut...) is shown as 1.234,56 under de. Done on the screen's text, after
 *  React renders it, so each feature keeps rendering the plain figure it reads from the server (as exports and
 *  audit lines carry it); when the locale goes back to en every figure changed here is put back. */
export function LocaleFigures({ locale, children }: { locale: Locale; children: ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const root = ref.current;
    if (!root || locale === 'en') return;
    // The text React rendered, for each text node shown differently here — to put back.
    const changed = new Map<Text, { plain: string; shown: string }>();

    const visit = (node: Text) => {
      const parent = node.parentElement;
      if (!parent || SKIP.has(parent.tagName)) return;
      const text = node.nodeValue ?? '';
      if ((parent.textContent ?? '').trim() !== text.trim()) return;
      const shown = formatFigure(text, locale);
      if (shown === text) return;
      changed.set(node, { plain: text, shown });
      node.nodeValue = shown;
    };
    const walk = (node: Node) => {
      if (node.nodeType === Node.TEXT_NODE) {
        visit(node as Text);
        return;
      }
      const texts = document.createTreeWalker(node, NodeFilter.SHOW_TEXT);
      for (let t = texts.nextNode(); t; t = texts.nextNode()) visit(t as Text);
    };

    walk(root);
    const observer = new MutationObserver((records) => {
      for (const r of records) {
        if (r.type === 'characterData') walk(r.target);
        else r.addedNodes.forEach(walk);
      }
    });
    observer.observe(root, { subtree: true, childList: true, characterData: true });
    return () => {
      observer.disconnect();
      for (const [node, { plain, shown }] of changed) {
        if (node.isConnected && node.nodeValue === shown) node.nodeValue = plain;
      }
    };
  }, [locale]);

  return (
    <div ref={ref} style={{ display: 'contents' }}>
      {children}
    </div>
  );
}
