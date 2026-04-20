import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function Topbar({ title, action = null, children = null }) {
  const { user } = useAuth()

  const getInitials = (name) => {
    if (!name) return 'U'
    return name.split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2)
  }

  return (
    <>
      <style>{`
        .topbar {
          height: var(--topbar-h, 64px); background: #fff;
          border-bottom: 1px solid var(--border, #e2e8f0);
          display: flex; align-items: center; padding: 0 24px; gap: 12px;
          position: sticky; top: 0; z-index: 50;
        }
        .topbar-left { font-weight: 600; font-size: 15px; }
        .topbar-right { margin-left: auto; display: flex; align-items: center; gap: 10px; }
        .btn-create {
          background: var(--accent, #3b82f6); color: white; border: none;
          border-radius: 8px; padding: 8px 16px; font-size: 13px; font-weight: 600;
          text-decoration: none; display: inline-flex; align-items: center; gap: 6px; transition: opacity .15s; cursor: pointer;
        }
        .btn-create:hover { opacity: .9; color: white; }
        .avatar {
          width: 34px; height: 34px; border-radius: 50%;
          background: var(--accent, #3b82f6); color: white;
          display: flex; align-items: center; justify-content: center;
          font-size: 13px; font-weight: 700; flex-shrink: 0;
        }
      `}</style>

      <div className="topbar">
        <div className="topbar-left">{title}</div>
        {children}
        <div className="topbar-right">
          {action}
          <div className="avatar">{getInitials(user?.username || user?.email || 'U')}</div>
        </div>
      </div>
    </>
  )
}
