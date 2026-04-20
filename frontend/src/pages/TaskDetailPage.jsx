import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import taskService from '../api/taskService'

export default function TaskDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [task, setTask] = useState(null)
  const [comments, setComments] = useState([])
  const [commentContent, setCommentContent] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    loadTask()
  }, [id])

  const loadTask = async () => {
    try {
      setLoading(true)
      setError('')
      console.log('Loading task with ID:', id)
      
      const [taskRes, commentsRes] = await Promise.all([
        taskService.getTaskById(id),
        taskService.getComments(id).catch(() => ({ data: [] })),
      ])
      
      console.log('Task response:', taskRes)
      
      const taskData = taskRes.data || taskRes
      setTask(taskData)
      setComments(commentsRes.data || commentsRes || [])
    } catch (err) {
      console.error('Failed to load task:', err)
      setError(`Lỗi tải công việc: ${err.response?.data?.message || err.message}`)
    } finally {
      setLoading(false)
    }
  }

  const handleAddComment = async (e) => {
    e.preventDefault()
    if (!commentContent) return

    try {
      await taskService.addComment(id, commentContent)
      setCommentContent('')
      loadTask()
    } catch (err) {
      alert('Thêm bình luận thất bại')
    }
  }

  const handleUpdateStatus = async (newStatus) => {
    try {
      await taskService.updateTaskStatus(id, newStatus)
      loadTask()
    } catch (err) {
      alert('Cập nhật trạng thái thất bại')
    }
  }

  if (loading) return <div className="text-center py-4"><i className="fas fa-spinner fa-spin"></i> Đang tải...</div>
  if (error) return <div className="text-center py-4" style={{ color: '#dc2626' }}><i className="fas fa-exclamation-circle"></i> {error}</div>
  if (!task) return <div className="text-center py-4">Không tìm thấy công việc. Hãy kiểm tra Browser Console (F12) để xem lỗi chi tiết.</div>

  return (
    <>
      <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet" />
      <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet" />
      <style>{`
        body { background: #f5f7fa; font-family: 'Segoe UI', sans-serif; }
        .navbar-custom { background: white; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
        .detail-container { max-width: 1000px; margin: 30px auto; padding: 20px; }
        .detail-card { background: white; border: 1px solid #e1e4e8; border-radius: 8px; padding: 30px; }
        .detail-title { font-size: 24px; font-weight: 700; color: #333; margin-bottom: 20px; }
        .detail-section { margin-bottom: 30px; padding-bottom: 20px; border-bottom: 1px solid #e1e4e8; }
        .detail-section:last-child { border-bottom: none; }
        .section-title { font-size: 16px; font-weight: 700; color: #333; margin-bottom: 10px; }
        .section-text { font-size: 14px; color: #666; }
        .badge { font-size: 12px; padding: 4px 8px; border-radius: 4px; display: inline-block; }
        .comment-item { padding: 12px; border: 1px solid #e1e4e8; border-radius: 6px; margin-bottom: 8px; }
        .comment-author { font-weight: 600; font-size: 13px; margin-bottom: 4px; }
        .comment-content { font-size: 13px; }
        .btn { padding: 8px 16px; border-radius: 6px; font-weight: 600; border: none; cursor: pointer; text-decoration: none; }
        .btn-primary { background: #667eea; color: white; }
        .btn-primary:hover { background: #5568d3; color: white; }
        .form-group { margin-bottom: 12px; }
        .form-control { width: 100%; padding: 8px 12px; border: 1px solid #e1e4e8; border-radius: 6px; font-size: 14px; }
      `}</style>

      <nav className="navbar-custom">
        <div style={{ padding: '1rem 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <button onClick={() => task.projectId ? navigate(`/projects/${task.projectId}`) : navigate('/tasks')} style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '16px', fontWeight: 'bold' }}>
            ← Quay lại
          </button>
          <Link to={`/tasks/${id}/edit`} className="btn btn-primary" style={{ fontSize: '13px' }}>Chỉnh sửa</Link>
        </div>
      </nav>

      <div className="detail-container">
        <div className="detail-card">
          <h1 className="detail-title">{task.title}</h1>

          <div className="detail-section">
            <div className="section-title">Mô tả</div>
            <p className="section-text">{task.description || 'Không có mô tả'}</p>
          </div>

          <div className="detail-section">
            <div className="section-title">Thông tin</div>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
              <div>
                <div style={{ fontSize: '12px', color: '#999', marginBottom: '4px' }}>Dự án</div>
                <div>{task.project?.name || '-'}</div>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#999', marginBottom: '4px' }}>Độ ưu tiên</div>
                <span className="badge" style={{ background: task.priority === 'HIGH' ? '#fee2e2' : task.priority === 'MEDIUM' ? '#fef3c7' : '#dcfce7', color: task.priority === 'HIGH' ? '#dc2626' : task.priority === 'MEDIUM' ? '#d97706' : '#16a34a' }}>
                  {task.priority}
                </span>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#999', marginBottom: '4px' }}>Trạng thái</div>
                <select onChange={(e) => handleUpdateStatus(e.target.value)} defaultValue={task.status} style={{ padding: '4px 8px', border: '1px solid #e1e4e8', borderRadius: '4px' }}>
                  <option value="TODO">Chưa bắt đầu</option>
                  <option value="DOING">Đang làm</option>
                  <option value="DONE">Hoàn thành</option>
                </select>
              </div>
              <div>
                <div style={{ fontSize: '12px', color: '#999', marginBottom: '4px' }}>Hạn chót</div>
                <div>{task.dueDate ? new Date(task.dueDate).toLocaleDateString('vi-VN') : '-'}</div>
              </div>
            </div>
          </div>

          <div className="detail-section">
            <div className="section-title">Bình luận ({comments.length})</div>
            {comments.length === 0 ? (
              <p className="section-text">Chưa có bình luận nào</p>
            ) : (
              <div style={{ marginBottom: '20px' }}>
                {comments.map((c) => (
                  <div key={c.id} className="comment-item">
                    <div className="comment-author">{c.user?.fullName || c.user?.username}</div>
                    <div className="comment-content">{c.content}</div>
                    <div style={{ fontSize: '11px', color: '#999', marginTop: '4px' }}>
                      {new Date(c.createdAt).toLocaleDateString('vi-VN')}
                    </div>
                  </div>
                ))}
              </div>
            )}

            <form onSubmit={handleAddComment}>
              <div className="form-group">
                <textarea
                  className="form-control"
                  placeholder="Thêm bình luận..."
                  value={commentContent}
                  onChange={(e) => setCommentContent(e.target.value)}
                  rows="3"
                />
              </div>
              <button type="submit" className="btn btn-primary" style={{ fontSize: '13px' }}>Gửi</button>
            </form>
          </div>
        </div>
      </div>
    </>
  )
}
