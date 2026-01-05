let eventIdCounter = 0;
let isStreaming = false;
let eventSource = null;

function startStreaming() {
    const taskInput = document.getElementById('taskInput');
    const resultContent = document.getElementById('resultContent');
    
    if (isStreaming) {
        alert('A task is already in progress. Please wait for it to complete.');
        return;
    }
    
    const task = taskInput.value.trim();
    if (!task) {
        alert('Please enter a task description.');
        return;
    }
    
    // Clear previous results
    resultContent.innerHTML = '';
    
    // Show loading state
    const loadingElement = document.createElement('div');
    loadingElement.className = 'event-card';
    loadingElement.innerHTML = `
        <div class="event-header">
            <div class="spinner-border" style="width: 24px; height: 24px; border-width: 2px;"></div>
            Processing your task...
        </div>
        <div class="event-content">
            <p>Please wait while we process your request. This may take a moment.</p>
        </div>
    `;
    resultContent.appendChild(loadingElement);
    
    // Scroll to bottom
    resultContent.scrollTop = resultContent.scrollHeight;
    
    isStreaming = true;
    
    // Start SSE connection
    eventSource = new EventSource(`/api/stream?task=${encodeURIComponent(task)}`);
    
    eventSource.onopen = () => {
        console.log('SSE connection opened');
    };
    
    eventSource.onmessage = (event) => {
        const data = JSON.parse(event.data);
        handleEvent(data);
    };
    
    eventSource.onerror = (error) => {
        console.error('SSE error:', error);
        if (eventSource) {
            eventSource.close();
        }
        isStreaming = false;
        
        const errorElement = document.createElement('div');
        errorElement.className = 'event-card error-event';
        errorElement.innerHTML = `
            <div class="event-header">
                Connection Error
            </div>
            <div class="event-content">
                <p>There was an error with the streaming connection. Please try again.</p>
            </div>
        `;
        resultContent.appendChild(errorElement);
        resultContent.scrollTop = resultContent.scrollHeight;
    };
}

function handleEvent(data) {
    const resultContent = document.getElementById('resultContent');
    
    // Remove loading indicator if present
    const loadingElement = resultContent.querySelector('.event-content:contains("Please wait")');
    if (loadingElement) {
        loadingElement.parentElement.remove();
    }
    
    const eventElement = document.createElement('div');
    eventElement.className = `event-card ${getEventClass(data.type)}`;
    eventElement.id = `event-${eventIdCounter++}`;
    
    if (data.type === 'TASK_COMPLETE') {
        eventElement.innerHTML = formatTaskComplete(data);
    } else if (data.type === 'TOOL_RESULT') {
        eventElement.innerHTML = formatToolResult(data);
    } else if (data.type === 'ITERATION') {
        eventElement.innerHTML = formatIteration(data);
    } else if (data.type === 'ERROR') {
        eventElement.innerHTML = formatError(data);
    }
    
    resultContent.appendChild(eventElement);
    resultContent.scrollTop = resultContent.scrollHeight;
    
    if (data.type === 'TASK_COMPLETE' || data.type === 'ERROR') {
        if (eventSource) {
            eventSource.close();
        }
        isStreaming = false;
    }
}

function getEventClass(type) {
    switch (type) {
        case 'TASK_COMPLETE': return 'complete-event';
        case 'TOOL_RESULT': return 'tool-result-event';
        case 'ITERATION': return 'iteration-event';
        case 'ERROR': return 'error-event';
        default: return '';
    }
}

function formatTaskComplete(data) {
    return `
        <div class="event-header">
            Task Completed Successfully
        </div>
        <div class="event-content">
            <p><strong>Task Summary:</strong> ${escapeHtml(data.message || 'Task completed successfully.')}</p>
            ${data.details ? `<p><strong>Details:</strong> ${escapeHtml(data.details)}</p>` : ''}
        </div>
    `;
}

function formatToolResult(data) {
    let paramsHtml = '';
    if (data.params && Object.keys(data.params).length > 0) {
        paramsHtml = `
            <div class="tool-params">
                <strong>Parameters:</strong>
                <pre>${JSON.stringify(data.params, null, 2)}</pre>
            </div>
        `;
    }
    
    let resultHtml = '';
    if (data.result) {
        if (data.tool_name === 'finish_task') {
            resultHtml = `
                <div class="finish-task-result">
                    <strong>Task Completion Summary:</strong>
                    <p>${escapeHtml(data.result)}</p>
                </div>
            `;
        } else {
            resultHtml = `
                <div class="tool-result">
                    <strong>Result:</strong>
                    <pre>${escapeHtml(data.result)}</pre>
                </div>
            `;
        }
    }
    
    return `
        <div class="event-header">
            <span class="tool-icon">🔧</span>
            <span class="tool-name">${escapeHtml(data.tool_name)}</span>
        </div>
        <div class="event-content">
            ${paramsHtml}
            ${resultHtml}
        </div>
    `;
}

function formatIteration(data) {
    return `
        <div class="event-header">
            <span class="iteration-icon">🔄</span>
            <span class="iteration-text">Iteration ${data.iteration}</span>
        </div>
        <div class="event-content">
            <p><strong>Thought:</strong> ${escapeHtml(data.thought)}</p>
            ${data.plan ? `<p><strong>Plan:</strong> ${escapeHtml(data.plan)}</p>` : ''}
        </div>
    `;
}

function formatError(data) {
    return `
        <div class="event-header">
            Error Encountered
        </div>
        <div class="event-content">
            <p><strong>Error:</strong> ${escapeHtml(data.message)}</p>
            ${data.details ? `<p><strong>Details:</strong> ${escapeHtml(data.details)}</p>` : ''}
        </div>
    `;
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function stopStreaming() {
    if (eventSource) {
        eventSource.close();
        eventSource = null;
    }
    isStreaming = false;
}

// Handle form submission
document.getElementById('taskForm').addEventListener('submit', function(e) {
    e.preventDefault();
    startStreaming();
});

// Handle page unload
window.addEventListener('beforeunload', function() {
    stopStreaming();
});