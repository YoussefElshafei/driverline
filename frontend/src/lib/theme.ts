import { useEffect, useState } from 'react'

export type ThemePreference = 'system' | 'light' | 'dark'

const STORAGE_KEY = 'driverline-theme'
const SIDEBAR_KEY = 'driverline-sidebar-collapsed'
const ORDER: ThemePreference[] = ['system', 'light', 'dark']

function readStored(): ThemePreference {
  try {
    const stored = localStorage.getItem(STORAGE_KEY)
    return stored === 'light' || stored === 'dark' ? stored : 'system'
  } catch {
    // Storage can be blocked (private windows, strict settings); the system theme is a fine fallback.
    return 'system'
  }
}

/**
 * The user's theme choice. "system" follows the operating system through the CSS media query; "light" and "dark"
 * set data-theme on <html>, which the stylesheet's token overrides key off.
 */
export function useTheme() {
  const [theme, setTheme] = useState<ThemePreference>(readStored)

  useEffect(() => {
    const root = document.documentElement
    if (theme === 'system') {
      delete root.dataset.theme
    } else {
      root.dataset.theme = theme
    }
    try {
      localStorage.setItem(STORAGE_KEY, theme)
    } catch {
      // Not remembering the choice is harmless.
    }
  }, [theme])

  function cycleTheme() {
    setTheme((current) => ORDER[(ORDER.indexOf(current) + 1) % ORDER.length])
  }

  return { theme, cycleTheme }
}

/** Whether the sidebar is collapsed to an icon rail, which gives wide tables (the 12-month forecast) more room. */
export function useSidebarCollapsed() {
  const [collapsed, setCollapsed] = useState(() => {
    try {
      return localStorage.getItem(SIDEBAR_KEY) === 'true'
    } catch {
      return false
    }
  })

  useEffect(() => {
    try {
      localStorage.setItem(SIDEBAR_KEY, String(collapsed))
    } catch {
      // Not remembering the choice is harmless.
    }
  }, [collapsed])

  return [collapsed, () => setCollapsed((current) => !current)] as const
}
