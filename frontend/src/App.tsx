import { NavLink, Navigate, Route, Routes } from 'react-router'
import { ActualsImportPage } from './features/actuals/ActualsImportPage'
import { BudgetEntryPage } from './features/budgets/BudgetEntryPage'
import { VariancePage } from './features/variance/VariancePage'

function App() {
  return (
    <div className="app">
      <header className="app-header">
        <div>
          <h1>DriverLine</h1>
          <p className="tagline">
            Budget vs. actual variance analysis and driver-based forecasting.
          </p>
        </div>
        <nav aria-label="Main">
          <NavLink to="/variance">Variance</NavLink>
          <NavLink to="/budgets">Budget entry</NavLink>
          <NavLink to="/actuals/import">Import actuals</NavLink>
        </nav>
      </header>
      <main>
        <Routes>
          <Route path="/" element={<Navigate to="/variance" replace />} />
          <Route path="/variance" element={<VariancePage />} />
          <Route path="/budgets" element={<BudgetEntryPage />} />
          <Route path="/actuals/import" element={<ActualsImportPage />} />
          <Route path="*" element={<p>Page not found.</p>} />
        </Routes>
      </main>
    </div>
  )
}

export default App
