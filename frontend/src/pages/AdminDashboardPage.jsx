import { useEffect, useState } from 'react'
import Sidebar from '../components/Sidebar'
import Topbar from '../components/Topbar'
import adminService from '../api/adminService'
import { useAuth } from '../context/AuthContext'

export default function AdminDashboardPage() {
  const { user: currentUser } = useAuth()
  const [stats, setStats] = useState({
    totalUsers: 0,
    totalProjects: 0,
    totalTasks: 0,
    taskStats: { TODO: 0, DOING: 0, DONE: 0 },
    overdueTasks: 0,
    overdueTasksList: [],
  })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadStats()
  }, [])

  const loadStats = async () => {
    try {
      setLoading(true)
      const res = await adminService.getDashboardStats()
      setStats(res.data || stats)
    } catch (err) {
      console.error('Failed to load stats', err)
    } finally {
      setLoading(false)
    }
  }

  // Calculate task percentages
  const totalTasksCount = stats.taskStats?.TODO + stats.taskStats?.DOING + stats.taskStats?.DONE || 1
  const todoPercent = totalTasksCount > 0 ? Math.round((stats.taskStats?.TODO || 0) / totalTasksCount * 100) : 0
  const doingPercent = totalTasksCount > 0 ? Math.round((stats.taskStats?.DOING || 0) / totalTasksCount * 100) : 0
  const donePercent = totalTasksCount > 0 ? Math.round((stats.taskStats?.DONE || 0) / totalTasksCount * 100) : 0

  // Format date
  const formatDate = (dateString) => {
    if (!dateString) return 'N/A'
    const date = new Date(dateString)
    return date.toLocaleDateString('vi-VN', { month: '2-digit', day: '2-digit' })
  }

  // Render pie chart
  const renderPieChart = () => {
    const radius = 80
    const circumference = 2 * Math.PI * radius

    const todoLength = circumference * (todoPercent / 100)
    const doingLength = circumference * (doingPercent / 100)
    const doneLength = circumference * (donePercent / 100)

    let offset = 0
    const circles = []

    // TODO (amber)
    circles.push(
      <circle
        key="todo"
        cx="100"
        cy="100"
        r={radius}
        fill="none"
        stroke="#fbbf24"
        strokeWidth="30"
        strokeDasharray={`${todoLength} ${circumference}`}
        strokeDashoffset={offset}
      />
    )
    offset -= todoLength

    // DOING (blue)
    circles.push(
      <circle
        key="doing"
        cx="100"
        cy="100"
        r={radius}
        fill="none"
        stroke="#3b82f6"
        strokeWidth="30"
        strokeDasharray={`${doingLength} ${circumference}`}
        strokeDashoffset={offset}
      />
    )
    offset -= doingLength

    // DONE (green)
    circles.push(
      <circle
        key="done"
        cx="100"
        cy="100"
        r={radius}
        fill="none"
        stroke="#10b981"
        strokeWidth="30"
        strokeDasharray={`${doneLength} ${circumference}`}
        strokeDashoffset={offset}
      />
    )

    return (
      <svg width="200" height="200" style={{ transform: 'rotate(-90deg)' }}>
        {circles}
      </svg>
    )
  }

  return (
    <>
      <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet" />
      <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet" />
      <style>{`
        :root {
          --sidebar-w: 240px;
          --sidebar-bg: #0f172a;
          --accent: #3b82f6;
          --topbar-h: 64px;
          --body-bg: #f1f5f9;
          --card-bg: #ffffff;
          --border: #e2e8f0;
          --text: #1e293b;
          --muted: #64748b;
          --success: #10b981;
          --warning: #f59e0b;
          --danger: #ef4444;
        }

        * { box-sizing: border-box; }
        body { background: var(--body-bg); font-family: 'Segoe UI', -apple-system, sans-serif; color: var(--text); margin: 0; }
        .main-wrapper { margin-left: var(--sidebar-w); min-height: 100vh; display: flex; flex-direction: column; }
        .page-content { padding: 24px; flex: 1; }
        .page-header { margin-bottom: 24px; }
        .page-header h3 { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
        .page-header p { font-size: 13px; color: var(--muted); margin: 0; }

        /* Stats Grid - 4 columns */
        .stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
        .stat-card {
          background: var(--card-bg); border: 1px solid var(--border);
          border-radius: 12px; padding: 20px;
          display: flex; flex-direction: column; gap: 8px;
          transition: box-shadow .15s;
        }
        .stat-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
        .stat-header { display: flex; align-items: center; gap: 10px; }
        .stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; font-size: 18px; flex-shrink: 0; }
        .stat-label { font-size: 13px; font-weight: 600; color: var(--text); }
        .stat-val { font-size: 28px; font-weight: 700; line-height: 1; margin: 0; }
        .stat-muted { font-size: 12px; color: var(--muted); margin: 0; }

        /* Main Content Grid - Chart & Task List */
        .content-grid { display: grid; grid-template-columns: 1.5fr 1fr; gap: 16px; }
        .chart-card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 12px; padding: 20px; }
        .task-list-card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 12px; padding: 20px; }
        
        .card-title { font-size: 15px; font-weight: 600; margin: 0 0 16px; display: flex; align-items: center; justify-content: space-between; }
        .view-all-link { font-size: 12px; color: var(--accent); text-decoration: none; font-weight: 600; }
        .view-all-link:hover { text-decoration: underline; }

        .chart-wrapper { display: flex; justify-content: center; align-items: center; min-height: 200px; }
        .legend { margin-top: 16px; }
        .legend-item { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; font-size: 12px; }
        .legend-color { width: 12px; height: 12px; border-radius: 2px; }
        .legend-count { font-weight: 600; color: var(--text); }
        .legend-percent { color: var(--muted); }

        /* Task List */
        .task-item { display: flex; align-items: flex-start; gap: 12px; padding: 12px 0; border-bottom: 1px solid var(--border); }
        .task-item:last-child { border-bottom: none; }
        .task-date { font-size: 11px; color: var(--danger); font-weight: 700; min-width: 40px; text-align: center; background: #fee2e2; padding: 2px 6px; border-radius: 4px; }
        .task-info { flex: 1; min-width: 0; }
        .task-title { font-size: 13px; font-weight: 600; color: var(--text); margin: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
        .task-project { font-size: 11px; color: var(--muted); margin: 2px 0 0; }
        .task-list-empty { text-align: center; padding: 40px 16px; color: var(--muted); }

        /* Responsive */
        @media (max-width: 1200px) {
          .stats-grid { grid-template-columns: repeat(2, 1fr); }
          .content-grid { grid-template-columns: 1fr; }
        }

        @media (max-width: 768px) {
          .stats-grid { grid-template-columns: 1fr; }
          .content-grid { grid-template-columns: 1fr; }
          .main-wrapper { margin-left: 0; }
        }
      `}</style>

      <Sidebar isAdmin={true} />
      <div className="main-wrapper">
        <Topbar title="Admin Dashboard" />

        <div className="page-content">
          <div className="page-header">
            <h3>Admin Dashboard Metrics</h3>
          </div>

          {loading ? (
            <div className="text-center py-4">
              <i className="fas fa-spinner fa-spin me-2"></i>Đang tải thống kê...
            </div>
          ) : (
            <>
              {/* Stats Grid - 4 Cards */}
              <div className="stats-grid">
                <div className="stat-card">
                  <div className="stat-header">
                    <div className="stat-icon" style={{ background: '#dbeafe' }}>
                      <i className="fas fa-users" style={{ color: '#2563eb' }}></i>
                    </div>
                    <span className="stat-label">Total Users</span>
                  </div>
                  <p className="stat-val">{stats.totalUsers}</p>
                  <p className="stat-muted">+4 this month</p>
                </div>

                <div className="stat-card">
                  <div className="stat-header">
                    <div className="stat-icon" style={{ background: '#dbeafe' }}>
                      <i className="fas fa-project-diagram" style={{ color: '#2563eb' }}></i>
                    </div>
                    <span className="stat-label">Total Projects</span>
                  </div>
                  <p className="stat-val">{stats.totalProjects}</p>
                  <p className="stat-muted">18 active</p>
                </div>

                <div className="stat-card">
                  <div className="stat-header">
                    <div className="stat-icon" style={{ background: '#fef3c7' }}>
                      <i className="fas fa-tasks" style={{ color: '#d97706' }}></i>
                    </div>
                    <span className="stat-label">Total Tasks</span>
                  </div>
                  <p className="stat-val">{stats.totalTasks}</p>
                  <p className="stat-muted">32 created today</p>
                </div>

                <div className="stat-card">
                  <div className="stat-header">
                    <div className="stat-icon" style={{ background: '#fee2e2' }}>
                      <i className="fas fa-exclamation-triangle" style={{ color: '#dc2626' }}></i>
                    </div>
                    <span className="stat-label">Overdue Tasks</span>
                  </div>
                  <p className="stat-val" style={{ color: '#dc2626' }}>{stats.overdueTasks}</p>
                  <p className="stat-muted">Past due tasks</p>
                </div>
              </div>

              {/* Content Grid - Chart & Task List */}
              <div className="content-grid">
                {/* Task Status Chart */}
                <div className="chart-card">
                  <div className="card-title">Task Status</div>
                  <div className="chart-wrapper">
                    <div>
                      <div style={{ display: 'flex', justifyContent: 'center', marginBottom: '16px' }}>
                        {renderPieChart()}
                      </div>
                      <div style={{ textAlign: 'center', marginBottom: '16px' }}>
                        <strong style={{ fontSize: '24px' }}>{donePercent}%</strong>
                        <div style={{ fontSize: '12px', color: 'var(--muted)' }}>Done</div>
                      </div>
                    </div>
                  </div>
                  <div className="legend">
                    <div className="legend-item">
                      <div className="legend-color" style={{ background: '#10b981' }}></div>
                      <span className="legend-count">{stats.taskStats?.DONE || 0}</span>
                      <span className="legend-percent">Done</span>
                    </div>
                    <div className="legend-item">
                      <div className="legend-color" style={{ background: '#3b82f6' }}></div>
                      <span className="legend-count">{stats.taskStats?.DOING || 0}</span>
                      <span className="legend-percent">Doing</span>
                    </div>
                    <div className="legend-item">
                      <div className="legend-color" style={{ background: '#fbbf24' }}></div>
                      <span className="legend-count">{stats.taskStats?.TODO || 0}</span>
                      <span className="legend-percent">Todo</span>
                    </div>
                  </div>
                </div>

                {/* Overdue Task List */}
                <div className="task-list-card">
                  <div className="card-title">
                    Overdue Task List
                    <a href="#" className="view-all-link">View All →</a>
                  </div>
                  {stats.overdueTasksList && stats.overdueTasksList.length > 0 ? (
                    <div className="task-list">
                      {stats.overdueTasksList.map((task) => (
                        <div key={task.id} className="task-item">
                          <div className="task-date">{formatDate(task.dueDate)}</div>
                          <div className="task-info">
                            <p className="task-title">{task.title}</p>
                            <p className="task-project">
                              {task.projectId ? `Project #${task.projectId}` : 'N/A'}
                            </p>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="task-list-empty">
                      <i className="fas fa-check-circle" style={{ fontSize: '24px', marginBottom: '8px', display: 'block', opacity: 0.5 }}></i>
                      <p>No overdue tasks</p>
                    </div>
                  )}
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </>
  )
}

