document.addEventListener('DOMContentLoaded', function() {
    const form = document.getElementById('agentForm');
    const resultSection = document.getElementById('resultSection');
    const resultContent = document.getElementById('resultContent');
    const loadingSpinner = document.getElementById('loadingSpinner');

    form.addEventListener('submit', async function(e) {
        e.preventDefault();
        
        const prompt = document.getElementById('prompt').value;
        const directoryPath = document.getElementById('directoryPath').value;
        const useCollaboration = document.getElementById('useCollaboration').checked;

        resultSection.style.display = 'block';
        resultContent.innerHTML = '<div class="stream-header">🔴 Live Response Stream</div>';
        loadingSpinner.style.display = 'block';

        try {
            const response = await fetch('/api/stream', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    prompt: prompt,
                    directoryPath: directoryPath,
                    useCollaboration: useCollaboration
                })
            });

            if (!response.ok) {
                throw new Error('Network response was not ok');
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';

            while (true) {
                const {done, value} = await reader.read();
                
                if (done) {
                    loadingSpinner.style.display = 'none';
                    break;
                }

                buffer += decoder.decode(value, {stream: true});
                const lines = buffer.split('\n');
                buffer = lines.pop();

                for (const line of lines) {
                    if (line.startsWith('data:')) {
                        const jsonStr = line.substring(5).trim();
                        if (jsonStr) {
                            try {
                                const event = JSON.parse(jsonStr);
                                displayEvent(event);
                            } catch (e) {
                                console.error('Error parsing event:', e, jsonStr);
                            }
                        }
                    }
                }
            }

        } catch (error) {
            console.error('Error:', error);
            loadingSpinner.style.display = 'none';
            resultContent.innerHTML += `
                <div class="event-card error-event">
                    <div class="event-header">❌ Error</div>
                    <div class="event-content">${escapeHtml(error.message)}</div>
                </div>
            `;
        }
    });

    function displayEvent(event) {
        let eventHtml = '';
        
        switch(event.type) {
            case 'ITERATION_START':
                eventHtml = `
                    <div class="event-card iteration-event">
                        <div class="event-header">
                            <span>🔄</span>
                            <span>Iteration ${event.iteration}</span>
                        </div>
                        <div class="event-content">${escapeHtml(event.message)}</div>
                    </div>
                `;
                break;
                
            case 'TOOL_RESULT':
                const toolIcon = getToolIcon(event.toolName);
                const resultText = formatToolResult(event.toolName, event.toolResult);
                eventHtml = `
                    <div class="event-card tool-result-event">
                        <div class="event-header">
                            <span>${toolIcon}</span>
                            <span>${escapeHtml(event.toolName)}</span>
                        </div>
                        <div class="event-content">${resultText}</div>
                    </div>
                `;
                break;
                
            case 'TASK_COMPLETE':
                eventHtml = `
                    <div class="event-card complete-event">
                        <div class="event-header">
                            <span>✅</span>
                            <span>Task Complete</span>
                        </div>
                        <div class="event-content">
                            <div class="task-complete-container">
                                <div class="task-complete-icon">🎉</div>
                                <h3 class="task-complete-heading">Task Successfully Completed!</h3>
                                <p class="task-complete-message">${escapeHtml(event.message)}</p>
                                <div class="task-complete-footer">
                                    <span class="task-complete-checkmark">✓</span>
                                    <span class="task-complete-status">All operations completed successfully</span>
                                </div>
                            </div>
                        </div>
                    </div>
                `;
                break;
                
            case 'ERROR':
                eventHtml = `
                    <div class="event-card error-event">
                        <div class="event-header">
                            <span>❌</span>
                            <span>Error</span>
                        </div>
                        <div class="event-content">
                            ${escapeHtml(event.message || event.error)}
                        </div>
                    </div>
                `;
                break;
        }
        
        if (eventHtml) {
            resultContent.insertAdjacentHTML('beforeend', eventHtml);
            resultContent.scrollTop = resultContent.scrollHeight;
        }
    }

    function getToolIcon(toolName) {
        const icons = {
            'write_file': '📝',
            'read_file': '📖',
            'list_files': '📂',
            'log_thought': '💭',
            'finish_task': '✅'
        };
        return icons[toolName] || '🔧';
    }

    function formatToolResult(toolName, result) {
        if (!result) return '';
        
        // For file operations, format nicely
        if (toolName === 'write_file' && result.startsWith('Success')) {
            const path = result.replace('Success: File written to ', '');
            return `<strong>✓ File created:</strong> <code>${escapeHtml(path)}</code>`;
        }
        
        if (toolName === 'read_file') {
            return `<strong>✓ File read successfully</strong> (content not displayed)`;
        }
        
        if (toolName === 'list_files') {
            // Format file list nicely
            const lines = result.split('\n');
            if (lines.length > 10) {
                const shown = lines.slice(0, 10);
                const remaining = lines.length - 10;
                return `<pre>${escapeHtml(shown.join('\n'))}\n... and ${remaining} more items</pre>`;
            }
            return `<pre>${escapeHtml(result)}</pre>`;
        }
        
        if (toolName === 'finish_task') {
            // Parse the JSON response for better formatting
            try {
                const response = JSON.parse(result);
                const summary = response.summary || 'Task completed successfully';
                const timestamp = response.timestamp || 'Just now';
                
                return `
                    <div class="finish-task-container">
                        <div class="finish-task-icon">🎉</div>
                        <h3 class="finish-task-heading">Task Complete!</h3>
                        <p class="finish-task-message">${escapeHtml(summary)}</p>
                        <div class="finish-task-timestamp">Completed at: ${escapeHtml(timestamp)}</div>
                        <div class="finish-task-footer">
                            <span class="finish-task-checkmark">✓</span>
                            <span class="finish-task-status">Operation completed successfully</span>
                        </div>
                    </div>
                `;
            } catch (e) {
                // Fallback to simple formatting if JSON parsing fails
                return `
                    <div class="finish-task-container">
                        <div class="finish-task-icon">🎉</div>
                        <h3 class="finish-task-heading">Task Complete!</h3>
                        <p class="finish-task-message">${escapeHtml(result)}</p>
                        <div class="finish-task-footer">
                            <span class="finish-task-checkmark">✓</span>
                            <span class="finish-task-status">Operation completed successfully</span>
                        </div>
                    </div>
                `;
            }
        }
        
        // Default formatting
        return `<pre>${escapeHtml(result)}</pre>`;
    }

    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
});