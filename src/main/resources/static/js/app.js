document.addEventListener('DOMContentLoaded', function() {
    const promptInput = document.getElementById('prompt');
    const directoryPathInput = document.getElementById('directoryPath');
    const submitBtn = document.getElementById('submitBtn');
    const btnText = submitBtn.querySelector('.btn-text');
    const spinner = submitBtn.querySelector('.spinner');
    const resultSection = document.getElementById('resultSection');
    const errorSection = document.getElementById('errorSection');
    const agentTypeElement = document.getElementById('agentType');
    const reasoningElement = document.getElementById('reasoning');
    const resultContentElement = document.getElementById('resultContent');
    const errorMessageElement = document.getElementById('errorMessage');

    submitBtn.addEventListener('click', handleSubmit);

    promptInput.addEventListener('keydown', function(e) {
        if (e.ctrlKey && e.key === 'Enter') {
            handleSubmit();
        }
    });

    async function handleSubmit() {
        const prompt = promptInput.value.trim();
        const directoryPath = directoryPathInput.value.trim();

        if (!prompt) {
            showError('Please enter a prompt');
            return;
        }

        hideResults();
        setLoading(true);

        try {
            const response = await fetch('/api/submit', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    prompt: prompt,
                    directoryPath: directoryPath || null
                })
            });

            if (!response.ok) {
                throw new Error(`Server error: ${response.status}`);
            }

            const data = await response.json();
            displayResult(data);
        } catch (error) {
            console.error('Error:', error);
            showError(`Failed to process request: ${error.message}`);
        } finally {
            setLoading(false);
        }
    }

    function setLoading(isLoading) {
        submitBtn.disabled = isLoading;
        if (isLoading) {
            btnText.style.display = 'none';
            spinner.style.display = 'inline-block';
        } else {
            btnText.style.display = 'inline';
            spinner.style.display = 'none';
        }
    }

    function hideResults() {
        resultSection.style.display = 'none';
        errorSection.style.display = 'none';
    }

    function displayResult(data) {
        agentTypeElement.textContent = data.agentType;
        agentTypeElement.className = `agent-badge ${data.agentType}`;
        reasoningElement.textContent = data.reasoning;
        
        const formattedContent = formatMarkdown(data.result);
        resultContentElement.innerHTML = formattedContent;

        resultSection.style.display = 'block';
        resultSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function showError(message) {
        errorMessageElement.textContent = message;
        errorSection.style.display = 'block';
        errorSection.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    }

    function formatMarkdown(text) {
        let html = text;

        html = html.replace(/```(\w+)?\n([\s\S]*?)```/g, function(match, lang, code) {
            const language = lang || 'text';
            return `<pre><code class="language-${language}">${escapeHtml(code.trim())}</code></pre>`;
        });

        html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

        html = html.replace(/^### (.*$)/gim, '<h3>$1</h3>');
        html = html.replace(/^## (.*$)/gim, '<h2>$1</h2>');
        html = html.replace(/^# (.*$)/gim, '<h1>$1</h1>');

        html = html.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
        html = html.replace(/\*([^*]+)\*/g, '<em>$1</em>');

        html = html.replace(/^\d+\.\s+(.*)$/gim, function(match, item) {
            return `<li>${item}</li>`;
        });
        html = html.replace(/(<li>.*<\/li>)/s, '<ol>$1</ol>');

        html = html.replace(/^[-*]\s+(.*)$/gim, function(match, item) {
            return `<li>${item}</li>`;
        });
        html = html.replace(/(<li>.*<\/li>)(?!<\/ol>)/gs, function(match) {
            if (!match.includes('<ol>')) {
                return '<ul>' + match + '</ul>';
            }
            return match;
        });

        html = html.replace(/\n\n/g, '</p><p>');
        html = '<p>' + html + '</p>';

        html = html.replace(/<p><\/p>/g, '');
        html = html.replace(/<p>(<h[1-3]>)/g, '$1');
        html = html.replace(/(<\/h[1-3]>)<\/p>/g, '$1');
        html = html.replace(/<p>(<pre>)/g, '$1');
        html = html.replace(/(<\/pre>)<\/p>/g, '$1');
        html = html.replace(/<p>(<ul>)/g, '$1');
        html = html.replace(/(<\/ul>)<\/p>/g, '$1');
        html = html.replace(/<p>(<ol>)/g, '$1');
        html = html.replace(/(<\/ol>)<\/p>/g, '$1');

        return html;
    }

    function escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }
});
