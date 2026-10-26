import { useLayoutEffect, useRef } from 'react';
import type { ReactNode } from 'react';
import { showFigure, type Locale } from './useLocale';

// Shows every figure under it in the user's locale: any element whose whole text is a number with two decimals
// (an amount, a due, a total, a rate, a share, a cut — whichever feature renders it). The features keep rendering
// figures as the app writes them (1234.56), so exports, audit lines and entries are untouched; this rewrites only
// the text on screen, and again whenever a feature re-renders one. Under en it changes nothing.
export function LocaleFigures({ locale, children }: { locale: Locale; children: ReactNode }) {
  const ref = useRef<HTMLDivElement>(null);
  useLayoutEffect(() => {
    const root = ref.current;
    if (!root || locale === 'en') return;
    // Each text node rewritten, with the text it now shows and the text the feature rendered.
    const shown = new Map<Text, { raw: string; text: string }>();
    const visit = (node: Text) => {
      const raw = node.nodeValue ?? '';
      const parent = node.parentElement;
      if (!parent || parent.tagName === 'TEXTAREA' || parent.tagName === 'SCRIPT' || parent.tagName === 'STYLE') return;
      if ((parent.textContent ?? '').trim() !== raw.trim()) return; // a figure only when it is the cell's whole text
      const text = showFigure(raw, locale);
      if (text === raw) return;
      shown.set(node, { raw, text });
      node.nodeValue = text;
    };
    const walk = (node: Node) => {
      if (node.nodeType === Node.TEXT_NODE) return visit(node as Text);
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
      // Back to the figures as rendered, wherever the feature has not re-rendered them since.
      shown.forEach(({ raw, text }, node) => {
        if (node.nodeValue === text) node.nodeValue = raw;
      });
    };
  }, [locale]);
  return (
    <div ref={ref} style={{ display: 'contents' }}>
      {children}
    </div>
  );
}
