import { Link, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Sidebar({ isAdmin = false }) {
  const { logout, user } = useAuth()
  const location = useLocation()

  const handleLogout = () => {
    logout()
    window.location.href = '/login'
  }

  const isActive = (path) => location.pathname === path

  return (
    <>
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
        .sidebar {
          position: fixed; left: 0; top: 0; bottom: 0;
          width: var(--sidebar-w); background: var(--sidebar-bg);
          display: flex; flex-direction: column; z-index: 100; overflow-y: auto;
        }
        .sidebar-brand { padding: 20px 20px 16px; border-bottom: 1px solid rgba(255,255,255,0.08); }
        .brand-name { font-size: 17px; font-weight: 700; color: #fff; margin: 0; }
        .brand-sub { font-size: 11px; color: #94a3b8; margin: 0; }
        .sidebar-nav { padding: 16px 12px; flex: 1; }
        .nav-label { font-size: 10px; font-weight: 600; color: #475569; text-transform: uppercase; letter-spacing: .05em; padding: 10px 8px 4px; margin: 0; }
        .s-link {
          display: flex; align-items: center; gap: 10px;
          padding: 9px 12px; border-radius: 8px;
          color: #94a3b8; text-decoration: none;
          font-size: 13.5px; font-weight: 500;
          transition: all .15s; margin-bottom: 2px;
        }
        .s-link:hover { background: rgba(255,255,255,0.08); color: #fff; }
        .s-link.active { background: var(--accent); color: #fff; }
        .s-link i { width: 18px; text-align: center; font-size: 13px; }
        .sidebar-footer { padding: 12px; border-top: 1px solid rgba(255,255,255,0.08); }
        .logout-btn {
          display: flex; align-items: center; gap: 10px;
          padding: 9px 12px; border-radius: 8px; color: #f87171;
          background: none; border: none;
          font-size: 13.5px; font-weight: 500; transition: all .15s; width: 100%; cursor: pointer;
        }
        .logout-btn:hover { background: rgba(248,113,113,0.1); color: #ef4444; }
      `}</style>

      <nav className="sidebar">
        <div className="sidebar-brand">
          <h2 className="brand-name"><i className="fas fa-check-square me-2" style={{ color: '#60a5fa' }}></i>TaskFlow</h2>
          <p className="brand-sub">{isAdmin ? 'Admin Panel' : user?.role || 'USER'}</p>
        </div>

        <div className="sidebar-nav">
          {/* Main Links */}
          <div className="nav-label">Menu</div>
          <Link to={isAdmin ? '/admin' : '/'} className={`s-link ${isActive(isAdmin ? '/admin' : '/') ? 'active' : ''}`}>
            <i className="fas fa-home"></i> Dashboard
          </Link>

          {!isAdmin && (
            <>
              <Link to="/projects" className={`s-link ${isActive('/projects') ? 'active' : ''}`}>
                <i className="fas fa-project-diagram"></i> Dự án
              </Link>
              <Link to="/tasks" className={`s-link ${isActive('/tasks') ? 'active' : ''}`}>
                <i className="fas fa-tasks"></i> Công việc
              </Link>
            </>
          )}

          {isAdmin && (
            <>
              <div className="nav-label" style={{ marginTop: '12px' }}>Quản lý</div>
              <Link to="/admin/users" className={`s-link ${isActive('/admin/users') ? 'active' : ''}`}>
                <i className="fas fa-users"></i> Quản lý Users
              </Link>
              <Link to="/admin/projects" className={`s-link ${isActive('/admin/projects') ? 'active' : ''}`}>
                <i className="fas fa-sitemap"></i> Quản lý Projects
              </Link>
              <Link to="/admin/tasks" className={`s-link ${isActive('/admin/tasks') ? 'active' : ''}`}>
                <i className="fas fa-list-check"></i> Quản lý Tasks
              </Link>
            </>
          )}

          <div className="nav-label" style={{ marginTop: '12px' }}>Tài khoản</div>
          <Link to="/profile" className={`s-link ${isActive('/profile') ? 'active' : ''}`}>
            <i className="fas fa-user"></i> Hồ sơ
          </Link>
        </div>

        <div className="sidebar-footer">
          <button className="logout-btn" onClick={handleLogout}>
            <i className="fas fa-sign-out-alt"></i> Đăng xuất
          </button>
        </div>
      </nav>
    </>
  )
}
