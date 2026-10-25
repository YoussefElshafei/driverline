import type { ReactNode } from 'react'

interface PageHeaderProps {
  id: string
  title: string
  description?: ReactNode
  actions?: ReactNode
}

/** The page title (an h2 the page section is labelled by), a one-line description, and page-level actions. */
export function PageHeader({
  id,
  title,
  description,
  actions,
}: PageHeaderProps) {
  return (
    <header className="page-header">
      <div>
        <h2 id={id}>{title}</h2>
        {description && <p className="page-description">{description}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </header>
  )
}
