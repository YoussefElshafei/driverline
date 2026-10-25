import {
  Building2,
  ChartColumn,
  FileSpreadsheet,
  Monitor,
  Moon,
  PanelLeftClose,
  PanelLeftOpen,
  Sun,
  TrendingUp,
  Upload,
} from 'lucide-react'
import { NavLink, Navigate, Route, Routes } from 'react-router'
import { ActualsImportPage } from './features/actuals/ActualsImportPage'
import { BudgetEntryPage } from './features/budgets/BudgetEntryPage'
import { ForecastPage } from './features/forecast/ForecastPage'
import { VariancePage } from './features/variance/VariancePage'
import { useSidebarCollapsed, useTheme } from './lib/theme'

const NAV_ITEMS = [
  { to: '/variance', label: 'Variance', icon: ChartColumn },
  { to: '/forecast', label: 'Forecast', icon: TrendingUp },
  { to: '/budgets', label: 'Budget entry', icon: FileSpreadsheet },
  { to: '/actuals/import', label: 'Import actuals', icon: Upload },
]

const THEME_LABELS = {
  system: { label: 'System theme', icon: Monitor },
  light: { label: 'Light theme', icon: Sun },
  dark: { label: 'Dark theme', icon: Moon },
}

function App() {
  const { theme, cycleTheme } = useTheme()
  const [collapsed, toggleCollapsed] = useSidebarCollapsed()
  const ThemeIcon = THEME_LABELS[theme].icon

  return (
    <div className={`shell${collapsed ? ' is-collapsed' : ''}`}>
      <aside className="sidebar app-header">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path
                d="M4 18 L9 12 L13 15 L20 6"
                stroke="currentColor"
                strokeWidth="2.5"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <circle cx="20" cy="6" r="2" fill="currentColor" />
            </svg>
          </span>
          <div className="sidebar-text">
            <h1>DriverLine</h1>
            <p className="brand-tagline">Budget, actuals & forecasts</p>
          </div>
        </div>

        <nav className="nav" aria-label="Main">
          <p className="nav-label">Planning</p>
          {NAV_ITEMS.map(({ to, label, icon: Icon }) => (
            <NavLink key={to} to={to} title={collapsed ? label : undefined}>
              <Icon size={17} strokeWidth={1.9} aria-hidden="true" />
              <span className="sidebar-text">{label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-footer">
          <div className="company-card">
            <span className="company-avatar" aria-hidden="true">
              <Building2 size={16} strokeWidth={1.9} />
            </span>
            <div className="sidebar-text">
              <p className="company-name">Ace Janitorial Service</p>
              <p className="company-meta">Fictional demo data</p>
            </div>
          </div>
          <button
            type="button"
            className="theme-toggle"
            onClick={cycleTheme}
            title="Switch theme"
          >
            <ThemeIcon size={15} aria-hidden="true" />
            <span className="sidebar-text">{THEME_LABELS[theme].label}</span>
          </button>
          <button
            type="button"
            className="theme-toggle collapse-toggle"
            onClick={toggleCollapsed}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            {collapsed ? (
              <PanelLeftOpen size={15} aria-hidden="true" />
            ) : (
              <PanelLeftClose size={15} aria-hidden="true" />
            )}
            <span className="sidebar-text" aria-hidden="true">
              Collapse
            </span>
          </button>
        </div>
      </aside>

      <main className="content">
        <Routes>
          <Route path="/" element={<Navigate to="/variance" replace />} />
          <Route path="/variance" element={<VariancePage />} />
          <Route path="/forecast" element={<ForecastPage />} />
          <Route path="/budgets" element={<BudgetEntryPage />} />
          <Route path="/actuals/import" element={<ActualsImportPage />} />
          <Route
            path="*"
            element={
              <div className="page">
                <p>Page not found.</p>
              </div>
            }
          />
        </Routes>
      </main>
    </div>
  )
}

export default App
