document.addEventListener('DOMContentLoaded', function() {
    const taskForm = document.getElementById('taskForm');
    const resultSection = document.getElementById('resultSection');
    const resultContent = document.getElementById('resultContent');
    const copyBtn = document.getElementById('copyResultBtn');

    if (taskForm) {
        taskForm.addEventListener('submit', async function(e) {
            e.preventDefault();

            const formData = new FormData(taskForm);
            const taskId = formData.get('taskId');

            if (!taskId) {
                alert('Please enter a task ID');
                return;
            }

            resultContent.innerHTML = '<div class="spinner"><div class="spinner-border"></div><p>Processing your request...</p></div>';

            try {
                const response = await fetch(`/api/tasks/${taskId}/execute`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ taskId })
                });

                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                const result = await response.json();

                if (result.success) {
                    resultContent.innerHTML = `
                        <div class="event-card complete-event">
                            <div class="event-header">
                                <span>🎉</span>
                                <span>Task Complete</span>
                            </div>
                            <div class="event-content">
                                <p>Task execution has been completed successfully!</p>
                                <div class="summary">
                                    ${result.summary}
                                </div>
                                ${result.additionalInfo ? `
                                    <div class="additional-info">
                                        <strong>Additional Information:</strong>
                                        <pre>${JSON.stringify(result.additionalInfo, null, 2)}</pre>
                                    </div>
                                ` : ''}
                            </div>
                        </div>
                    `;
                } else {
                    resultContent.innerHTML = `
                        <div class="event-card error-event">
                            <div class="event-header">
                                <span>❌</span>
                                <span>Error</span>
                            </div>
                            <div class="event-content">
                                <p><strong>Error:</strong> ${result.message}</p>
                                ${result.details ? `
                                    <div class="error-details">
                                        <strong>Details:</strong>
                                        <pre>${JSON.stringify(result.details, null, 2)}</pre>
                                    </div>
                                ` : ''}
                            </div>
                        </div>
                    `;
                }

                resultSection.style.display = 'block';

            } catch (error) {
                console.error('Error:', error);
                resultContent.innerHTML = `
                    <div class="event-card error-event">
                        <div class="event-header">
                            <span>❌</span>
                            <span>Error</span>
                        </div>
                        <div class="event-content">
                            <p><strong>Error:</strong> ${error.message}</p>
                        </div>
                    </div>
                `;
                resultSection.style.display = 'block';
            }
        });
    }

    if (copyBtn) {
        copyBtn.addEventListener('click', function() {
            const resultText = resultContent.innerText;
            navigator.clipboard.writeText(resultText).then(() => {
                alert('Result copied to clipboard!');
            });
        });
    }
});