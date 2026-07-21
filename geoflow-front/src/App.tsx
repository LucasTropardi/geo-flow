import axios from 'axios'
import { useEffect, useRef, useState } from 'react'
import { GeoJSON, MapContainer, TileLayer } from 'react-leaflet'
import { BrowserRouter, Navigate, Route, Routes, useNavigate, useParams } from 'react-router-dom'
import './App.css'

type JobStatus = 'PENDING' | 'RETRY_PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELED'
type JobType = 'BBOX_TO_GEOJSON'

type CreateJobResponse = {
  id: number
  status: JobStatus
  correlationId: string
  attemptCount: number
  maxAttempts: number
  createdAt: string
}

type JobResponse = {
  id: number
  tenantId: string
  name: string
  jobType: JobType
  status: JobStatus
  correlationId: string | null
  attemptCount: number
  maxAttempts: number
  area: {
    minLon: number
    minLat: number
    maxLon: number
    maxLat: number
  }
  resultGeojson: string | null
  errorMessage: string | null
  createdAt: string
  startedAt: string | null
  finishedAt: string | null
  updatedAt: string
}

type JobLogResponse = {
  id: number
  jobId: number
  correlationId: string | null
  level: string
  step: string
  message: string
  createdAt: string
}

type JobNotificationStatusResponse = {
  jobId: number
  status: JobStatus
}

type SubmitFormState = {
  tenantId: string
  name: string
  lonA: string
  latA: string
  lonB: string
  latB: string
}

const api = axios.create({
  baseURL: '/api',
})

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<SubmitJobPage />} />
        <Route path="/jobs/:jobId/waiting" element={<WaitingJobPage />} />
        <Route path="/jobs/:jobId/result" element={<JobResultPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}

function SubmitJobPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState<SubmitFormState>({
    tenantId: 'demo-farm 2',
    name: 'BBOX via UI',
    lonA: '-51.13',
    latA: '-19.093',
    lonB: '-49.156',
    latB: '-18.013',
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setSubmitting(true)
    setError(null)

    try {
      const lonA = Number(form.lonA)
      const lonB = Number(form.lonB)
      const latA = Number(form.latA)
      const latB = Number(form.latB)

      if ([lonA, lonB, latA, latB].some((value) => Number.isNaN(value))) {
        throw new Error('Preencha longitude e latitude com valores numericos validos.')
      }

      const payload = {
        tenantId: form.tenantId,
        name: form.name,
        jobType: 'BBOX_TO_GEOJSON',
        area: {
          minLon: Math.min(lonA, lonB),
          minLat: Math.min(latA, latB),
          maxLon: Math.max(lonA, lonB),
          maxLat: Math.max(latA, latB),
        },
      }

      const response = await api.post<CreateJobResponse>('/jobs', payload)
      navigate(`/jobs/${response.data.id}/waiting`)
    } catch (requestError) {
      setError(extractErrorMessage(requestError))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="shell">
      <section className="hero-panel">
        <div className="hero-copy">
          <span className="eyebrow">GeoFlow</span>
          <h1>Informe dois cantos opostos da area</h1>
          <p>
            Voce adiciona dois cantos opostos quaisquer da area. O front normaliza min e max,
            envia o job e espera o notifier avisar quando o processamento terminar.
          </p>
        </div>
      </section>

      <section className="page-grid">
        <form className="card form-card" onSubmit={handleSubmit}>
          <div className="section-head">
            <h2>Novo Job</h2>
            <p>Use dois cantos da area. A ordem nao importa.</p>
          </div>

          <label className="field">
            <span>Tenant ID</span>
            <input
              value={form.tenantId}
              onChange={(event) => setForm({ ...form, tenantId: event.target.value })}
              required
            />
          </label>

          <label className="field">
            <span>Nome do job</span>
            <input
              value={form.name}
              onChange={(event) => setForm({ ...form, name: event.target.value })}
              required
            />
          </label>

          <div className="corner-grid">
            <div className="corner-card">
              <h3>Canto A</h3>
              <label className="field">
                <span>Longitude</span>
                <input
                  value={form.lonA}
                  onChange={(event) => setForm({ ...form, lonA: event.target.value })}
                  required
                />
              </label>
              <label className="field">
                <span>Latitude</span>
                <input
                  value={form.latA}
                  onChange={(event) => setForm({ ...form, latA: event.target.value })}
                  required
                />
              </label>
            </div>

            <div className="corner-card">
              <h3>Canto B</h3>
              <label className="field">
                <span>Longitude</span>
                <input
                  value={form.lonB}
                  onChange={(event) => setForm({ ...form, lonB: event.target.value })}
                  required
                />
              </label>
              <label className="field">
                <span>Latitude</span>
                <input
                  value={form.latB}
                  onChange={(event) => setForm({ ...form, latB: event.target.value })}
                  required
                />
              </label>
            </div>
          </div>

          <div className="preview-box">
            <strong>Area normalizada</strong>
            <code>{formatNormalizedArea(form)}</code>
          </div>

          {error ? <div className="alert error">{error}</div> : null}

          <button className="primary-button" type="submit" disabled={submitting}>
            {submitting ? 'Enviando...' : 'Criar job e aguardar notifier'}
          </button>
        </form>
      </section>
    </main>
  )
}

function WaitingJobPage() {
  const navigate = useNavigate()
  const { jobId } = useParams()
  const [message, setMessage] = useState('Conectando ao notifier...')
  const eventSourceRef = useRef<EventSource | null>(null)

  useEffect(() => {
    if (!jobId) {
      navigate('/', { replace: true })
      return
    }

    const eventSource = new EventSource(`/notifications/jobs/${jobId}/stream`)
    eventSourceRef.current = eventSource

    eventSource.addEventListener('subscribed', (event) => {
      const payload = JSON.parse((event as MessageEvent<string>).data) as JobNotificationStatusResponse
      setMessage(waitingStatusMessage(payload.jobId, payload.status))
    })

    eventSource.addEventListener('job-status', (event) => {
      const payload = JSON.parse((event as MessageEvent<string>).data) as JobNotificationStatusResponse
      setMessage(`Job ${payload.jobId} terminou com status ${payload.status}. Redirecionando...`)
      eventSource.close()
      navigate(`/jobs/${payload.jobId}/result`, { replace: true })
    })

    eventSource.onerror = () => {
      setMessage('A conexao com o notifier falhou. Voce pode voltar e reenviar, ou abrir o resultado depois.')
      eventSource.close()
    }

    return () => {
      eventSource.close()
    }
  }, [jobId, navigate])

  return (
    <main className="shell waiting-shell">
      <section className="card waiting-card">
        <span className="eyebrow">Aguardando SSE</span>
        <h1>Processando job {jobId}</h1>
        <p>{message}</p>
        <div className="pulse-line" />
        <button className="secondary-button" type="button" onClick={() => navigate('/')}>
          Voltar para o formulario
        </button>
      </section>
    </main>
  )
}

function JobResultPage() {
  const { jobId } = useParams()
  const navigate = useNavigate()
  const [job, setJob] = useState<JobResponse | null>(null)
  const [logs, setLogs] = useState<JobLogResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reprocessing, setReprocessing] = useState(false)

  useEffect(() => {
    if (!jobId) {
      navigate('/', { replace: true })
      return
    }

    let active = true

    async function load() {
      try {
        const [jobResponse, logResponse] = await Promise.all([
          api.get<JobResponse>(`/jobs/${jobId}`),
          api.get<JobLogResponse[]>(`/jobs/${jobId}/logs`),
        ])

        if (!active) {
          return
        }

        setJob(jobResponse.data)
        setLogs(logResponse.data)
      } catch (requestError) {
        if (active) {
          setError(extractErrorMessage(requestError))
        }
      } finally {
        if (active) {
          setLoading(false)
        }
      }
    }

    load()

    return () => {
      active = false
    }
  }, [jobId, navigate])

  if (loading) {
    return (
      <main className="shell waiting-shell">
        <section className="card waiting-card">
          <h1>Carregando resultado do job {jobId}</h1>
        </section>
      </main>
    )
  }

  if (error || !job) {
    return (
      <main className="shell waiting-shell">
        <section className="card waiting-card">
          <h1>Falha ao carregar resultado</h1>
          <p>{error ?? 'Job nao encontrado.'}</p>
          <button className="secondary-button" type="button" onClick={() => navigate('/')}>
            Voltar
          </button>
        </section>
      </main>
    )
  }

  const parsedGeoJson = job.resultGeojson ? safeParseGeoJson(job.resultGeojson) : null
  const formattedGeoJson = formatGeoJson(job.resultGeojson)

  async function handleReprocess() {
    setReprocessing(true)
    setError(null)

    try {
      const response = await api.post<JobResponse>(`/jobs/${job.id}/reprocess`)
      navigate(`/jobs/${response.data.id}/waiting`, { replace: true })
    } catch (requestError) {
      setError(extractErrorMessage(requestError))
      setReprocessing(false)
    }
  }

  return (
    <main className="shell">
      <section className="result-header">
        <div>
          <span className="eyebrow">Resultado final</span>
          <h1>{job.name}</h1>
          <p>
            Job {job.id} para <strong>{job.tenantId}</strong> terminou com status{' '}
            <strong>{job.status}</strong>.
          </p>
        </div>
        <div className="result-actions">
          {job.status === 'FAILED' ? (
            <button
              className="primary-button"
              type="button"
              onClick={handleReprocess}
              disabled={reprocessing}
            >
              {reprocessing ? 'Reprocessando...' : 'Reprocessar job'}
            </button>
          ) : null}
          <button className="secondary-button" type="button" onClick={() => navigate('/')}>
            Novo job
          </button>
        </div>
      </section>

      {error ? <section className="card failure-card"><p>{error}</p></section> : null}

      {job.status === 'FAILED' ? (
        <section className="card failure-card">
          <h2>Processamento falhou</h2>
          <p>{job.errorMessage ?? 'Nenhuma mensagem de erro foi retornada.'}</p>
        </section>
      ) : null}

      <section className="result-grid">
        <section className="card map-card">
          <div className="section-head">
            <h2>Mapa</h2>
            <p>GeoJSON renderizado com Leaflet.</p>
          </div>

          {parsedGeoJson ? (
            <MapContainer
              className="leaflet-map"
              bounds={[
                [job.area.minLat, job.area.minLon],
                [job.area.maxLat, job.area.maxLon],
              ]}
              scrollWheelZoom
            >
              <TileLayer
                attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />
              <GeoJSON data={parsedGeoJson} />
            </MapContainer>
          ) : (
            <div className="empty-state">Sem GeoJSON para renderizar.</div>
          )}
        </section>

        <section className="card json-card">
          <div className="section-head">
            <h2>GeoJSON</h2>
            <p>Resultado formatado para leitura rapida.</p>
          </div>
          {job.resultGeojson ? (
            <pre>{formattedGeoJson}</pre>
          ) : (
            <div className="json-empty-state">Nenhum GeoJSON gerado.</div>
          )}
        </section>

        <section className="card logs-card">
          <div className="section-head">
            <h2>Rastreabilidade e logs</h2>
          </div>
          <div className="trace-panel">
            <div className="trace-field trace-field-wide">
              <span>Correlation ID</span>
              <strong>{job.correlationId ?? 'Nao informado'}</strong>
            </div>
            <div className="trace-field">
              <span>Tentativas</span>
              <strong>
                {job.attemptCount} de {job.maxAttempts}
              </strong>
            </div>
            <div className="trace-field">
              <span>Criado em</span>
              <strong>{formatDateTime(job.createdAt)}</strong>
            </div>
            <div className="trace-field">
              <span>Atualizado em</span>
              <strong>{formatDateTime(job.updatedAt)}</strong>
            </div>
          </div>
          <div className="logs-list">
            {logs.map((log) => (
              <article key={log.id} className="log-item">
                <div className="log-head">
                  <strong className="log-step">{log.step}</strong>
                  <div className="log-head-meta">
                    <span className="log-id">#{log.id}</span>
                    <time className="log-time">{formatDateTime(log.createdAt)}</time>
                  </div>
                </div>
                <div className="log-body">
                  <p className="log-message">{log.message}</p>
                </div>
              </article>
            ))}
          </div>
        </section>
      </section>
    </main>
  )
}

function formatNormalizedArea(form: SubmitFormState) {
  const lonA = Number(form.lonA)
  const lonB = Number(form.lonB)
  const latA = Number(form.latA)
  const latB = Number(form.latB)

  if ([lonA, lonB, latA, latB].some((value) => Number.isNaN(value))) {
    return 'Preencha os quatro valores para ver a area normalizada.'
  }

  return JSON.stringify(
    {
      minLon: Math.min(lonA, lonB),
      minLat: Math.min(latA, latB),
      maxLon: Math.max(lonA, lonB),
      maxLat: Math.max(latA, latB),
    },
    null,
    2,
  )
}

function safeParseGeoJson(value: string) {
  try {
    return JSON.parse(value)
  } catch {
    return null
  }
}

function formatGeoJson(value: string | null) {
  if (!value) {
    return 'Nenhum GeoJSON gerado.'
  }

  const parsedValue = safeParseGeoJson(value)

  if (!parsedValue) {
    return value
  }

  return JSON.stringify(parsedValue, null, 2)
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'medium',
    timeZone: 'America/Sao_Paulo',
  }).format(new Date(value))
}

function waitingStatusMessage(jobId: number, status: JobStatus) {
  if (status === 'RETRY_PENDING') {
    return `Job ${jobId} falhou em uma tentativa e ja esta aguardando reprocessamento automatico.`
  }

  return `Job ${jobId} ainda esta em ${status}. Esperando conclusao...`
}

function extractErrorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const responseMessage = error.response?.data?.message
    if (typeof responseMessage === 'string') {
      return responseMessage
    }
    return error.message
  }

  if (error instanceof Error) {
    return error.message
  }

  return 'Ocorreu um erro inesperado.'
}

export default App
