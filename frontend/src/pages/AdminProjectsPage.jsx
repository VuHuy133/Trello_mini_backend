import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import Sidebar from '../components/Sidebar'
import Topbar from '../components/Topbar'
import adminService from '../api/adminService'

export default function AdminProjectsPage() {
  const [projects, setProjects] = useState([])
  const [loading, setLoading] = useState(true)
  const [success, setSuccess] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [pageSize, setPageSize] = useState(20)

  useEffect(() => {
    loadProjects(0)
  }, [])

  const loadProjects = async (pageNum = 0) => {
    try {
      setLoading(true)
      const res = await adminService.getProjects({ page: pageNum, size: pageSize })
      setProjects(res.data?.content || [])
      setTotalPages(res.data?.totalPages || 0)
      setPage(pageNum)
    } catch (err) {
      console.error('Failed to load projects', err)
    } finally {
      setLoading(false)
    }
  }

  const goToPage = (pageNum) => {
    if (pageNum >= 0 && pageNum < totalPages) {
      loadProjects(pageNum)
    }
  }

  const renderPaginationButtons = () => {
    const buttons = []
    const showPages = 5
    let startPage = Math.max(0, page - 2)
    let endPage = Math.min(totalPages, startPage + showPages)
    if (endPage - startPage < showPages) {
      startPage = Math.max(0, endPage - showPages)
    }

    if (startPage > 0) {
      buttons.push(
        <button key="first" onClick={() => goToPage(0)} style={{ ...paginationBtn, cursor: 'pointer' }}>«</button>,
        startPage > 1 && <span key="dots1" style={{ ...paginationBtn, cursor: 'default' }}>...</span>
      )
    }

    for (let i = startPage; i < endPage; i++) {
      buttons.push(
        <button
          key={i}
          onClick={() => goToPage(i)}
          style={{
            ...paginationBtn,
            background: page === i ? '#3b82f6' : '#ffffff',
            color: page === i ? '#ffffff' : '#1e293b',
            cursor: 'pointer',
            fontWeight: page === i ? 700 : 500
          }}
        >
          {i + 1}
        </button>
      )
    }

    if (endPage < totalPages) {
      buttons.push(
        endPage < totalPages - 1 && <span key="dots2" style={{ ...paginationBtn, cursor: 'default' }}>...</span>,
        <button key="last" onClick={() => goToPage(totalPages - 1)} style={{ ...paginationBtn, cursor: 'pointer' }}>»</button>
      )
    }

    return buttons
  }

  const paginationBtn = {
    padding: '8px 12px',
    border: '2px solid #3b82f6',
    borderRadius: '8px',
    marginRight: '4px',
    fontSize: '13px',
    fontWeight: 500,
    background: '#ffffff',
    color: '#1e293b'
  }

  const handleDelete = async (id, name) => {
    if (!confirm(`Bạn có chắc chắn muốn xóa project "${name}"?`)) return

    try {
      await adminService.deleteProject(id)
      setSuccess('Xóa project thành công')
      loadProjects(page)
      setTimeout(() => setSuccess(''), 3000)
    } catch (err) {
      alert('Xóa project thất bại')
    }
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
        }
        * { box-sizing: border-box; }
        body { background: var(--body-bg); font-family: 'Segoe UI', -apple-system, sans-serif; color: var(--text); margin: 0; }
        .main-wrapper { margin-left: var(--sidebar-w); min-height: 100vh; display: flex; flex-direction: column; }
        .page-content { padding: 24px; flex: 1; }
        .page-header { margin-bottom: 24px; }
        .page-header h3 { font-size: 22px; font-weight: 700; margin: 0 0 4px; }
        .page-header p { font-size: 13px; color: var(--muted); margin: 0; }
        .sec-card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 12px; padding: 20px; overflow: hidden; }
        .data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
        .data-table th { background: #f8fafc; font-size: 11px; font-weight: 600; color: var(--muted); text-transform: uppercase; letter-spacing: .04em; padding: 12px; border-bottom: 2px solid var(--border); }
        .data-table td { padding: 12px; border-bottom: 1px solid #f1f5f9; vertical-align: middle; }
        .data-table tr:hover td { background: #f8fafc; }
        .badge { font-size: 11px; padding: 2px 8px; border-radius: 6px; display: inline-block; }
        .btn-sm { padding: 4px 12px; font-size: 12px; border-radius: 6px; }
        .alert { padding: 12px; border-radius: 6px; margin-bottom: 20px; }
        .alert-success { background: #dcfce7; color: #166534; }
      `}</style>

      <Sidebar isAdmin={true} />
      <div className="main-wrapper">
        <Topbar title="Quản lý Projects" />

        <div className="page-content">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
            <div>
              <h3 style={{ fontSize: '20px', fontWeight: 700, margin: '0' }}>Quản lý Projects {totalPages > 1 && `(Trang ${page + 1}/${totalPages})`}</h3>
              <p style={{ fontSize: '13px', color: 'var(--muted)', margin: '4px 0 0' }}>{Array.isArray(projects) ? projects.length : 0} projects</p>
            </div>
          </div>

          {success && <div className="alert alert-success"><i className="fas fa-check-circle me-2"></i>{success}</div>}

          {loading ? (
            <div className="text-center py-4">
              <i className="fas fa-spinner fa-spin me-2"></i>Đang tải...
            </div>
          ) : (
            <>
              <div className="sec-card">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th style={{ paddingLeft: '12px' }}>ID</th>
                      <th>Tên dự án</th>
                      <th>Mô tả</th>
                      <th>Type</th>
                      <th style={{ textAlign: 'right', paddingRight: '12px' }}>Thao tác</th>
                    </tr>
                  </thead>
                  <tbody>
                    {projects.map((p) => (
                      <tr key={p.id}>
                        <td style={{ paddingLeft: '12px' }}>{p.id}</td>
                        <td><Link to={`/projects/${p.id}`} style={{ textDecoration: 'none', color: 'inherit', fontWeight: 600 }}>{p.name}</Link></td>
                        <td><span style={{ fontSize: '12px', color: 'var(--muted)' }}>{p.description ? (p.description.length > 40 ? p.description.substring(0, 40) + '...' : p.description) : '-'}</span></td>
                        <td><span className="badge" style={{ background: '#f1f5f9', color: '#64748b' }}>{p.type}</span></td>
                        <td style={{ textAlign: 'right', paddingRight: '12px' }}>
                          <Link to={`/projects/${p.id}`} className="btn btn-sm btn-outline-secondary" style={{ marginRight: '4px' }}>Xem</Link>
                          <Link to={`/projects/${p.id}/edit`} className="btn btn-sm btn-outline-primary" style={{ marginRight: '4px' }}>Sửa</Link>
                          <button className="btn btn-sm btn-outline-danger" onClick={() => handleDelete(p.id, p.name)}>Xóa</button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {totalPages > 1 && (
                <div style={{ display: 'flex', gap: '4px', justifyContent: 'center', marginTop: '20px', flexWrap: 'wrap' }}>
                  {renderPaginationButtons()}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </>
  )
}
