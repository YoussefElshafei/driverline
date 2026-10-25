import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
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
    expect(screen.getByRole('link', { name: 'Variance' })).toHaveAttribute(
      'href',
      '/variance',
    )
    expect(screen.getByRole('link', { name: 'Forecast' })).toHaveAttribute(
      'href',
      '/forecast',
    )
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

  it('collapses the sidebar to icons while keeping the links named', async () => {
    const user = userEvent.setup()
    const { container } = renderAt('/actuals/import')

    await user.click(screen.getByRole('button', { name: 'Collapse sidebar' }))

    expect(container.querySelector('.shell')).toHaveClass('is-collapsed')
    expect(screen.getByRole('link', { name: 'Forecast' })).toBeInTheDocument()
    expect(
      screen.getByRole('button', { name: 'Expand sidebar' }),
    ).toBeInTheDocument()
  })

  it('shows a not-found message for unknown routes', () => {
    renderAt('/nope')

    expect(screen.getByText('Page not found.')).toBeInTheDocument()
  })
})
