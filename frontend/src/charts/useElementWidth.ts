import { useEffect, useRef, useState } from 'react'

/** Tracks an element's width so SVG charts can draw at 1:1 pixels (crisp hairlines, readable text) when resized. */
export function useElementWidth<T extends HTMLElement>(fallback = 720) {
  const ref = useRef<T>(null)
  const [width, setWidth] = useState(fallback)

  useEffect(() => {
    const element = ref.current
    // jsdom (tests) and very old browsers have no ResizeObserver; the fallback width is fine there.
    if (!element || typeof ResizeObserver === 'undefined') {
      return
    }
    const observer = new ResizeObserver(([entry]) => {
      const next = Math.floor(entry.contentRect.width)
      if (next > 0) setWidth(next)
    })
    observer.observe(element)
    return () => observer.disconnect()
  }, [])

  return [ref, width] as const
}
