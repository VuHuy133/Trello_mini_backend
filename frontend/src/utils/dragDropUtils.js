/**
 * Drag and Drop utility functions for task reordering
 */

/**
 * Reorder items within the same list
 * @param {Array} list - The list of items
 * @param {number} startIndex - Starting index
 * @param {number} endIndex - Ending index
 * @returns {Array} - New reordered list
 */
export const reorderItemsWithinList = (list, startIndex, endIndex) => {
  const result = Array.from(list);
  const [removed] = result.splice(startIndex, 1);
  result.splice(endIndex, 0, removed);
  return result;
};

/**
 * Move item across different lists
 * @param {Array} sourceList - Source list
 * @param {Array} destList - Destination list
 * @param {number} sourceIndex - Index in source list
 * @param {number} destIndex - Index in destination list
 * @returns {Object} - Object with updated source and destination lists
 */
export const moveItemAcrossLists = (sourceList, destList, sourceIndex, destIndex) => {
  const sourceResult = Array.from(sourceList);
  const destResult = Array.from(destList);
  const [removed] = sourceResult.splice(sourceIndex, 1);
  destResult.splice(destIndex, 0, removed);

  return {
    source: sourceResult,
    destination: destResult,
  };
};

/**
 * Calculate new positions after reordering
 * @param {Array} tasks - Array of tasks in order
 * @returns {Array} - Tasks with updated position values
 */
export const calculatePositions = (tasks) => {
  return tasks.map((task, index) => ({
    ...task,
    position: index,
  }));
};

/**
 * Group tasks by status
 * @param {Array} tasks - Array of tasks
 * @returns {Object} - Tasks grouped by status
 */
export const groupTasksByStatus = (tasks) => {
  const statusGroups = {
    TODO: [],
    DOING: [],
    DONE: [],
  };

  tasks.forEach((task) => {
    const status = task.status || 'TODO';
    if (statusGroups[status]) {
      statusGroups[status].push(task);
    }
  });

  // Sort each status group by position
  Object.keys(statusGroups).forEach((status) => {
    statusGroups[status].sort((a, b) => {
      const posA = a.position !== null && a.position !== undefined ? a.position : Infinity;
      const posB = b.position !== null && b.position !== undefined ? b.position : Infinity;
      return posA - posB;
    });
  });

  return statusGroups;
};

/**
 * Build reorder payload for API
 * @param {Object} statusGroups - Grouped tasks by status
 * @returns {Array} - Array of ReorderTaskItem objects
 */
export const buildReorderPayload = (statusGroups) => {
  const payload = [];
  
  Object.keys(statusGroups).forEach((status) => {
    statusGroups[status].forEach((task, position) => {
      payload.push({
        id: task.id,
        status: status,
        position: position,
      });
    });
  });

  return payload;
};

/**
 * Get droppable ID for a status
 * @param {string} status - Task status
 * @returns {string} - Droppable ID
 */
export const getDroppableId = (status) => {
  return `status-${status}`;
};

/**
 * Get status from droppable ID
 * @param {string} droppableId - Droppable ID
 * @returns {string} - Status
 */
export const getStatusFromDroppableId = (droppableId) => {
  return droppableId.replace('status-', '');
};

/**
 * Get draggable ID for a task
 * @param {number} taskId - Task ID
 * @returns {string} - Draggable ID
 */
export const getDraggableId = (taskId) => {
  return `task-${taskId}`;
};

/**
 * Get task ID from draggable ID
 * @param {string} draggableId - Draggable ID
 * @returns {number} - Task ID
 */
export const getTaskIdFromDraggableId = (draggableId) => {
  return parseInt(draggableId.replace('task-', ''), 10);
};
