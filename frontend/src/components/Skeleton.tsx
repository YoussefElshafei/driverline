/** Placeholder shapes shown only on a page's first load; later reloads keep the previous content dimmed instead. */
export function Skeleton({
  width = '100%',
  height = 14,
}: {
  width?: number | string
  height?: number
}) {
  return (
    <span className="skeleton" style={{ width, height }} aria-hidden="true" />
  )
}

export function ReportSkeleton({ label }: { label: string }) {
  return (
    <div className="skeleton-stack" role="status" aria-label={label}>
      <div className="summary-cards">
        {[0, 1, 2].map((i) => (
          <div key={i} className="summary-card">
            <Skeleton width={90} height={12} />
            <Skeleton width={180} height={28} />
            <Skeleton width={140} height={12} />
          </div>
        ))}
      </div>
      <div className="card card-body skeleton-stack">
        <Skeleton height={220} />
      </div>
      <div className="card card-body skeleton-stack">
        {[0, 1, 2, 3, 4].map((i) => (
          <Skeleton key={i} height={16} />
        ))}
      </div>
    </div>
  )
}
