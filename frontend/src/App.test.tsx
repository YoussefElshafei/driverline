import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import App from './App'

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

describe('App', () => {
  it('renders the application name and main navigation', () => {
    renderAt('/actuals/import')

    expect(
      screen.getByRole('heading', { level: 1, name: 'DriverLine' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Budget entry' })).toHaveAttribute(
      'href',
      '/budgets',
    )
    expect(
      screen.getByRole('link', { name: 'Import actuals' }),
    ).toHaveAttribute('href', '/actuals/import')
  })

  it('shows the page for the current route', () => {
    renderAt('/actuals/import')

    expect(
      screen.getByRole('heading', { level: 2, name: 'Import actuals' }),
    ).toBeInTheDocument()
  })

  it('shows a not-found message for unknown routes', () => {
    renderAt('/nope')

    expect(screen.getByText('Page not found.')).toBeInTheDocument()
  })
})
