import { useEffect, useState } from 'react'
import { useNavigate, useParams, useLocation } from 'react-router-dom'
import projectService from '../api/projectService'

export default function ProjectFormPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const location = useLocation()
  const isEdit = location.pathname.includes('/edit')

  const [formData, setFormData] = useState({
    name: '',
    description: '',
    type: 'PUBLIC',
  })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (isEdit && id) {
      loadProject()
    }
  }, [id])

  const loadProject = async () => {
    try {
      setLoading(true)
      const res = await projectService.getProjectById(id)
      setFormData({
        name: res.data?.name || '',
        description: res.data?.description || '',
        type: res.data?.type || 'PUBLIC',
      })
    } catch (err) {
      setError('Không thể tải dự án')
    } finally {
      setLoading(false)
    }
  }

  const handleChange = (e) => {
    const { name, value } = e.target
    setFormData(prev => ({ ...prev, [name]: value }))
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')

    try {
      setLoading(true)
      if (isEdit && id) {
        await projectService.updateProject(id, formData)
        navigate(`/projects/${id}`)
      } else {
        await projectService.createProject(formData)
        navigate('/projects')
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Có lỗi xảy ra')
    } finally {
      setLoading(false)
    }
  }

  return (
    <>
      <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet" />
      <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet" />
      <style>{`
        body {
          background: linear-gradient(135deg, #f0f9ff 0%, #e0e7ff 100%);
          font-family: 'Segoe UI', -apple-system, sans-serif;
        }
        .navbar-custom { background: white; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
        .form-container { max-width: 800px; margin: 30px auto; padding: 20px; }
        .form-card {
          background: white; border: 1px solid #e1e4e8;
          border-radius: 8px; padding: 30px;
        }
        .form-title { font-size: 24px; font-weight: 700; color: #333; margin-bottom: 30px; }
        .form-group { margin-bottom: 20px; }
        .form-label { font-weight: 600; color: #333; margin-bottom: 8px; display: block; }
        .form-control, .form-select, textarea {
          border: 1px solid #e1e4e8; border-radius: 6px;
          padding: 10px 12px; font-size: 14px; width: 100%;
        }
        .form-control:focus, .form-select:focus, textarea:focus {
          border-color: #667eea; box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1); outline: none;
        }
        textarea { resize: vertical; min-height: 120px; font-family: inherit; }
        .form-actions {
          display: flex; gap: 10px; justify-content: flex-end;
          margin-top: 30px; padding-top: 20px; border-top: 1px solid #e1e4e8;
        }
        .btn {
          padding: 10px 20px; border-radius: 6px; font-weight: 600;
          border: none; cursor: pointer; transition: all 0.3s;
        }
        .btn-primary { background: #667eea; color: white; }
        .btn-primary:hover { background: #5568d3; color: white; }
        .btn-secondary { background: #e9ecef; color: #333; }
        .btn-secondary:hover { background: #dee2e6; color: #333; }
        .help-text { font-size: 12px; color: #999; margin-top: 5px; }
        .alert { padding: 12px; border-radius: 6px; margin-bottom: 20px; }
        .alert-danger { background: #fee2e2; color: #991b1b; }
      `}</style>

      <nav className="navbar-custom">
        <div style={{ padding: '1rem 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <button onClick={() => navigate('/projects')} style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px', fontWeight: 'bold' }}>
            ← Quay lại
          </button>
        </div>
      </nav>

      <div className="form-container">
        <div className="form-card">
          <h1 className="form-title">{isEdit ? 'Chỉnh sửa dự án' : 'Tạo dự án mới'}</h1>

          {error && <div className="alert alert-danger"><i className="fas fa-exclamation-circle me-2"></i>{error}</div>}

          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label className="form-label">Tên dự án</label>
              <input
                type="text"
                className="form-control"
                name="name"
                required
                value={formData.name}
                onChange={handleChange}
                placeholder="Nhập tên dự án..."
              />
              <div className="help-text">Tên dự án nên ngắn gọn và mô tả rõ nội dung</div>
            </div>

            <div className="form-group">
              <label className="form-label">Mô tả chi tiết</label>
              <textarea
                className="form-control"
                name="description"
                value={formData.description}
                onChange={handleChange}
                placeholder="Nhập mô tả dự án..."
              ></textarea>
              <div className="help-text">Mô tả chi tiết mục tiêu và phạm vi của dự án</div>
            </div>

            <div className="form-group">
              <label className="form-label">Loại dự án</label>
              <select
                className="form-select"
                name="type"
                value={formData.type}
                onChange={handleChange}
              >
                <option value="PUBLIC">Public</option>
                <option value="PRIVATE">Private</option>
              </select>
            </div>

            <div className="form-actions">
              <button type="button" className="btn btn-secondary" onClick={() => navigate('/projects')}>
                Hủy
              </button>
              <button type="submit" className="btn btn-primary" disabled={loading}>
                {loading ? 'Đang lưu...' : (isEdit ? 'Cập nhật dự án' : 'Tạo dự án')}
              </button>
            </div>
          </form>
        </div>
      </div>
    </>
  )
}
