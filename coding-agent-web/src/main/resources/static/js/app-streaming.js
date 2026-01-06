let eventSource;
let isStreaming = false;

function startStreaming() {
    if (isStreaming) {
        return;
    }

    const taskId = document.getElementById('taskId').value;
    const resultContent = document.getElementById('resultContent');

    if (!taskId) {
        alert('Please enter a task ID');
        return;
    }

    resultContent.innerHTML = '<div class="empty-state"><div class="empty-state-icon">🔄</div><p>Loading task events...</p></div>';

    eventSource = new EventSource(`/api/tasks/${taskId}/stream`);

    eventSource.onopen = () => {
        isStreaming = true;
        console.log('Connection to server opened');
    };

    eventSource.onmessage = (event) => {
        const data = JSON.parse(event.data);
        displayEvent(data);
    };

    eventSource.onerror = (error) => {
        console.error('EventSource failed:', error);
        resultContent.innerHTML += '<div class="event-card error-event"><div class="event-header">Error</div><div class="event-content">Failed to connect to the event stream. Please try again.</div></div>';
        stopStreaming();
    };
}

function stopStreaming() {
    if (eventSource) {
        eventSource.close();
        eventSource = null;
        isStreaming = false;
        console.log('Connection closed');
    }
}

function displayEvent(data) {
    const resultContent = document.getElementById('resultContent');

    if (data.eventType === 'TASK_STARTED') {
        const eventCard = document.createElement('div');
        eventCard.className = 'event-card iteration-event';
        eventCard.innerHTML = `
            <div class="event-header">
                <span>🚀</span>
                <span>Task Started</span>
            </div>
            <div class="event-content">
                <p>Task execution has started.</p>
            </div>
        `;
        resultContent.appendChild(eventCard);
    }

    else if (data.eventType === 'ITERATION_STARTED') {
        const eventCard = document.createElement('div');
        eventCard.className = 'event-card iteration-event';
        eventCard.innerHTML = `
            <div class="event-header">
                <span>🔄</span>
                <span>Iteration ${data.iteration}</span>
            </div>
            <div class="event-content">
                <p>Starting iteration ${data.iteration}.</p>
            </div>
        `;
        resultContent.appendChild(eventCard);
    }

    else if (data.eventType === 'TOOL_CALL') {
        const eventCard = document.createElement('div');
        eventCard.className = 'event-card tool-call-event';
        eventCard.innerHTML = `
            <div class="event-header">
                <span>🛠️</span>
                <span>${data.toolName}</span>
            </div>
            <div class="event-content">
                <p>Calling tool: ${data.toolName}</p>
                <div class="tool-params">
                    <strong>Parameters:</strong>
                    <pre>${JSON.stringify(data.parameters, null, 2)}</pre>
                </div>
            </div>
        `;
        resultContent.appendChild(eventCard);
    }

    else if (data.eventType === 'TOOL_RESULT') {
        const eventCard = document.createElement('div');
        eventCard.className = 'event-card tool-result-event';
        eventCard.innerHTML = `
            <div class="event-header">
                <span>✅</span>
                <span>${data.toolName} Result</span>
            </div>
            <div class="event-content">
                <p>Tool execution completed: ${data.toolName}</p>
                <div class="result-summary">
                    ${data.result}
                </div>
                ${data.details ? `
                    <div class="result-details">
                        <strong>Details:</strong>
                        <pre>${JSON.stringify(data.details, null, 2)}</pre>
                    </div>
                ` : ''}
            </div>
        `;
        resultContent.appendChild(eventCard);
    }

    else if (data.eventType === 'TASK_COMPLETE') {
        const eventCard = document.createElement('div');
        eventCard.className = 'event-card complete-event';
        eventCard.innerHTML = `
            <div class="event-header">
                <span>🎉</span>
                <span>Task Complete</span>
            </div>
            <div class="event-content">
                <p>Task execution has been completed successfully!</p>
                <div class="summary">
                    ${data.summary}
                </div>
                ${data.additionalInfo ? `
                    <div class="additional-info">
                        <strong>Additional Information:</strong>
                        <pre>${JSON.stringify(data.additionalInfo, null, 2)}</pre>
                    </div>
                ` : ''}
            </div>
        `;
        resultContent.appendChild(eventCard);
        stopStreaming();
    }

    else if (data.eventType === 'ERROR') {
        const eventCard = document.createElement('div');
        eventCard.className = 'event-card error-event';
        eventCard.innerHTML = `
            <div class="event-header">
                <span>❌</span>
                <span>Error</span>
            </div>
            <div class="event-content">
                <p><strong>Error:</strong> ${data.message}</p>
                ${data.details ? `
                    <div class="error-details">
                        <strong>Details:</strong>
                        <pre>${JSON.stringify(data.details, null, 2)}</pre>
                    </div>
                ` : ''}
            </div>
        `;
        resultContent.appendChild(eventCard);
        stopStreaming();
    }

    else {
        const eventCard = document.createElement('div');
        eventCard.className = 'event-card';
        eventCard.innerHTML = `
            <div class="event-header">
                <span>ℹ️</span>
                <span>Unknown Event</span>
            </div>
            <div class="event-content">
                <pre>${JSON.stringify(data, null, 2)}</pre>
            </div>
        `;
        resultContent.appendChild(eventCard);
    }

    resultContent.scrollTop = resultContent.scrollHeight;
}

function copyToClipboard() {
    const taskId = document.getElementById('taskId').value;
    if (taskId) {
        navigator.clipboard.writeText(taskId).then(() => {
            alert('Task ID copied to clipboard!');
        });
    }
}

function generateTaskId() {
    const taskId = 'task-' + Math.random().toString(36).substr(2, 9);
    document.getElementById('taskId').value = taskId;
    copyToClipboard();
}

// Initialize the page
document.addEventListener('DOMContentLoaded', () => {
    const startBtn = document.getElementById('startStreamingBtn');
    const stopBtn = document.getElementById('stopStreamingBtn');
    const copyBtn = document.getElementById('copyTaskIdBtn');
    const generateBtn = document.getElementById('generateTaskIdBtn');

    if (startBtn) {
        startBtn.addEventListener('click', startStreaming);
    }

    if (stopBtn) {
        stopBtn.addEventListener('click', stopStreaming);
    }

    if (copyBtn) {
        copyBtn.addEventListener('click', copyToClipboard);
    }

    if (generateBtn) {
        generateBtn.addEventListener('click', generateTaskId);
    }
});