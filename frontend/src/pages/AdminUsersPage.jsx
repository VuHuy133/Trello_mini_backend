import { useEffect, useState } from 'react'
import Sidebar from '../components/Sidebar'
import Topbar from '../components/Topbar'
import adminService from '../api/adminService'
import { useAuth } from '../context/AuthContext'

export default function AdminUsersPage() {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [pageSize, setPageSize] = useState(20)
  const { user: currentUser } = useAuth()

  useEffect(() => {
    loadUsers(0)
  }, [])

  const loadUsers = async (pageNum = 0) => {
    try {
      setLoading(true)
      const res = await adminService.getUsers({ page: pageNum, size: pageSize })
      setUsers(res.data?.content || [])
      setTotalPages(res.data?.totalPages || 0)
      setPage(pageNum)
    } catch (err) {
      console.error('Failed to load users', err)
    } finally {
      setLoading(false)
    }
  }

  const goToPage = (pageNum) => {
    if (pageNum >= 0 && pageNum < totalPages) {
      loadUsers(pageNum)
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

  const handleChangeRole = async (id, currentRole) => {
    const newRole = currentRole === 'ADMIN' ? 'USER' : 'ADMIN'
    const label = newRole === 'ADMIN' ? 'nâng lên ADMIN' : 'hạ xuống USER'
    if (!confirm(`Bạn có chắc muốn ${label} user này?`)) return
    try {
      await adminService.changeUserRole(id, newRole)
      loadUsers(page)
    } catch (err) {
      alert('Thay đổi role thất bại')
    }
  }

  const handleDelete = async (id, email) => {
    if (!confirm(`Bạn có chắc chắn muốn xóa user "${email}"?`)) return
    try {
      await adminService.deleteUser(id)
      loadUsers(page === 0 ? 0 : page)
    } catch (err) {
      alert('Xóa user thất bại')
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
        .sec-card { background: var(--card-bg); border: 1px solid var(--border); border-radius: 12px; padding: 20px; overflow: hidden; }
        .data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
        .data-table th { background: #f8fafc; font-size: 11px; font-weight: 600; color: var(--muted); text-transform: uppercase; letter-spacing: .04em; padding: 12px; border-bottom: 2px solid var(--border); }
        .data-table td { padding: 12px; border-bottom: 1px solid #f1f5f9; vertical-align: middle; }
        .data-table tr:hover td { background: #f8fafc; }
        .badge { font-size: 11px; padding: 2px 8px; border-radius: 6px; }
        .btn-sm { padding: 4px 12px; font-size: 12px; }
        .pagination-container { display: flex; align-items: center; justify-content: center; gap: 4px; margin-top: 20px; }
        .pagination-btn { padding: 8px 12px; border: 2px solid #3b82f6; border-radius: 8px; font-size: 13px; cursor: pointer; background: #ffffff; color: #1e293b; }
      `}</style>

      <Sidebar isAdmin={true} />
      <div className="main-wrapper">
        <Topbar title="Quản lý Users" />

        <div className="page-content">
          <div style={{ marginBottom: '24px' }}>
            <h3 style={{ fontSize: '20px', fontWeight: 700, margin: 0 }}>Quản lý Users</h3>
            <p style={{ fontSize: '13px', color: 'var(--muted)', margin: '4px 0 0' }}>
              {Array.isArray(users) ? users.length : 0} users {totalPages > 0 && `(Trang ${page + 1}/${totalPages})`}
            </p>
          </div>

          {loading ? (
            <div className="text-center py-4">
              <i className="fas fa-spinner fa-spin me-2"></i>Đang tải...
            </div>
          ) : (
            <div className="sec-card">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Họ tên</th>
                    <th>Email</th>
                    <th>Username</th>
                    <th>Role</th>
                    <th>Ngày tạo</th>
                    <th style={{ textAlign: 'right' }}>Thao tác</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => (
                    <tr key={u.id}>
                      <td>{u.id}</td>
                      <td>
                        {u.fullName || u.username}
                        {u.id === currentUser?.id && (
                          <span style={{ background: '#e0e7ff', color: '#4338ca', fontSize: '10px', padding: '1px 6px', borderRadius: '4px', marginLeft: '6px', fontWeight: 600 }}>Bạn</span>
                        )}
                      </td>
                      <td>{u.email}</td>
                      <td>{u.username}</td>
                      <td>
                        <span className="badge" style={{ background: u.role === 'ADMIN' ? '#fee2e2' : '#dbeafe', color: u.role === 'ADMIN' ? '#dc2626' : '#1d4ed8' }}>
                          {u.role}
                        </span>
                      </td>
                      <td>{u.createdAt ? new Date(u.createdAt).toLocaleDateString('vi-VN') : '-'}</td>
                      <td style={{ textAlign: 'right' }}>
                        {u.id !== currentUser?.id && (
                          <button
                            className={u.role === 'ADMIN' ? 'btn btn-sm btn-outline-secondary' : 'btn btn-sm btn-outline-warning'}
                            onClick={() => handleChangeRole(u.id, u.role)}
                            style={{ marginRight: '4px' }}
                          >
                            {u.role === 'ADMIN' ? 'Hạ xuống USER' : 'Nâng lên ADMIN'}
                          </button>
                        )}
                        {u.id !== currentUser?.id && (
                          <button className="btn btn-sm btn-outline-danger" onClick={() => handleDelete(u.id, u.email)}>
                            Xóa
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {totalPages > 1 && (
                <div className="pagination-container">
                  {renderPaginationButtons()}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </>
  )
}
