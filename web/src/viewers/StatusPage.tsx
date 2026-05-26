import './StatusPage.css'

interface StatusPageProps {
  title: string
  subtitle?: string
  error?: string
}

export function StatusPage({ title, subtitle, error }: StatusPageProps) {
  if (error) {
    return (
      <div className="error-box">
        <h3>{title}</h3>
        <pre>{error}</pre>
      </div>
    )
  }

  return (
    <div className="status-page">
      <h2>{title}</h2>
      {subtitle && <p>{subtitle}</p>}
    </div>
  )
}
