// Configuration
const CONFIG = {
    RULES_API_BASE: 'http://localhost:8082/api/rules'
};

// State
let rules = [];
let editingRuleId = null;
let isLoading = false;

// DOM Elements
const elements = {
    tableBody: document.getElementById('rulesTableBody'),
    refreshBtn: document.getElementById('refreshRulesBtn'),
    inferBtn: document.getElementById('inferRulesBtn'),
    form: document.getElementById('ruleForm'),
    formTitle: document.getElementById('formTitle'),
    formAlert: document.getElementById('formAlert'),
    submitBtn: document.getElementById('submitBtn'),
    cancelEditBtn: document.getElementById('cancelEditBtn'),
    inputs: {
        id: document.getElementById('ruleId'),
        ruleName: document.getElementById('ruleName'),
        ruleType: document.getElementById('ruleType'),
        thresholdValue: document.getElementById('thresholdValue'),
        operator: document.getElementById('operator'),
        priority: document.getElementById('priority'),
        importance: document.getElementById('importance'),
        description: document.getElementById('description'),
        failureMessage: document.getElementById('failureMessage'),
        enabled: document.getElementById('enabled')
    }
};

// Initialize
function init() {
    elements.refreshBtn.addEventListener('click', loadRules);
    elements.inferBtn.addEventListener('click', inferRulesFromModel);
    elements.form.addEventListener('submit', handleFormSubmit);
    elements.cancelEditBtn.addEventListener('click', resetForm);
    loadRules();
}

// API Helpers
async function apiRequest(path, options = {}) {
    const url = `${CONFIG.RULES_API_BASE}${path}`;
    try {
        const response = await fetch(url, {
            headers: {
                'Content-Type': 'application/json'
            },
            ...options
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || `Request failed with status ${response.status}`);
        }

        if (response.status === 204) {
            return null;
        }

        const data = await response.json();
        return data;
    } catch (error) {
        console.error('API request failed:', error);
        throw error;
    }
}

async function loadRules() {
    if (isLoading) return;
    isLoading = true;
    setTableLoadingState(true);

    try {
        const data = await apiRequest('', { method: 'GET' });
        rules = Array.isArray(data) ? data.sort(sortRules) : [];
        renderRules();
        setAlert('Rules refreshed successfully.', 'success');
    } catch (error) {
        renderErrorState('Failed to load rules. Please try again.');
        setAlert(parseErrorMessage(error), 'error');
    } finally {
        isLoading = false;
        setTableLoadingState(false);
    }
}

function sortRules(a, b) {
    // Sort by priority ascending, then ruleName
    const priorityDiff = (a.priority ?? 999) - (b.priority ?? 999);
    if (priorityDiff !== 0) {
        return priorityDiff;
    }
    return (a.ruleName || '').localeCompare(b.ruleName || '');
}

function renderRules() {
    if (!rules.length) {
        elements.tableBody.innerHTML = `
            <tr>
                <td colspan="11" class="empty-state">No rules configured yet. Create your first rule using the form below.</td>
            </tr>
        `;
        return;
    }

    const rows = rules.map(rule => `
        <tr data-rule-id="${rule.id}">
            <td>
                <div class="rule-name">${escapeHtml(rule.ruleName)}</div>
                <div class="rule-description">${escapeHtml(rule.description || '')}</div>
            </td>
            <td><span class="badge">${escapeHtml(rule.ruleType)}</span></td>
            <td>${formatThreshold(rule.thresholdValue)}</td>
            <td>${escapeHtml(rule.operator)}</td>
            <td>${rule.priority ?? '-'}</td>
            <td><span class="importance ${formatImportanceClass(rule.importance)}">${escapeHtml(rule.importance || 'N/A')}</span></td>
            <td>
                <button class="status-toggle ${rule.enabled ? 'enabled' : 'disabled'}" data-action="toggle" data-enabled="${rule.enabled}">
                    ${rule.enabled ? 'Enabled' : 'Disabled'}
                </button>
            </td>
            <td>${escapeHtml(rule.source || 'MANUAL')}</td>
            <td>${formatConfidence(rule.confidenceScore)}</td>
            <td>
                <div class="timestamp">${formatDate(rule.updatedAt || rule.createdAt)}</div>
            </td>
            <td class="actions">
                <button class="action-btn" data-action="edit">Edit</button>
                <button class="action-btn danger" data-action="delete">Delete</button>
            </td>
        </tr>
    `).join('');

    elements.tableBody.innerHTML = rows;

    // Attach event listeners for actions
    elements.tableBody.querySelectorAll('button[data-action="edit"]').forEach(button => {
        button.addEventListener('click', handleEditClick);
    });

    elements.tableBody.querySelectorAll('button[data-action="delete"]').forEach(button => {
        button.addEventListener('click', handleDeleteClick);
    });

    elements.tableBody.querySelectorAll('button[data-action="toggle"]').forEach(button => {
        button.addEventListener('click', handleToggleClick);
    });
}

function setTableLoadingState(isLoadingState) {
    if (isLoadingState) {
        elements.tableBody.innerHTML = `
            <tr>
                <td colspan="11" class="empty-state">Loading rules...</td>
            </tr>
        `;
    }
}

function renderErrorState(message) {
    elements.tableBody.innerHTML = `
        <tr>
            <td colspan="11" class="empty-state error">${escapeHtml(message)}</td>
        </tr>
    `;
}

function handleEditClick(event) {
    const row = event.target.closest('tr');
    const ruleId = Number(row.dataset.ruleId);
    const rule = rules.find(r => r.id === ruleId);
    if (!rule) {
        setAlert('Rule not found for editing.', 'error');
        return;
    }
    populateForm(rule);
}

function handleDeleteClick(event) {
    const row = event.target.closest('tr');
    const ruleId = Number(row.dataset.ruleId);
    const rule = rules.find(r => r.id === ruleId);
    if (!rule) {
        setAlert('Rule not found for deletion.', 'error');
        return;
    }

    if (!confirm(`Are you sure you want to delete rule "${rule.ruleName}"? This action cannot be undone.`)) {
        return;
    }

    deleteRule(ruleId);
}

function handleToggleClick(event) {
    const row = event.target.closest('tr');
    const ruleId = Number(row.dataset.ruleId);
    const rule = rules.find(r => r.id === ruleId);
    if (!rule) {
        setAlert('Rule not found for toggle.', 'error');
        return;
    }

    toggleRule(ruleId, !rule.enabled);
}

async function handleFormSubmit(event) {
    event.preventDefault();

    const payload = collectFormData();
    if (!payload) {
        return;
    }

    elements.submitBtn.disabled = true;
    elements.submitBtn.textContent = editingRuleId ? 'Updating...' : 'Creating...';

    try {
        if (editingRuleId) {
            await apiRequest(`/${editingRuleId}`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });
            setAlert(`Rule "${payload.ruleName}" updated successfully.`, 'success');
        } else {
            await apiRequest('', {
                method: 'POST',
                body: JSON.stringify(payload)
            });
            setAlert(`Rule "${payload.ruleName}" created successfully.`, 'success');
        }
        await loadRules();
        resetForm();
    } catch (error) {
        setAlert(parseErrorMessage(error), 'error');
    } finally {
        elements.submitBtn.disabled = false;
        elements.submitBtn.textContent = editingRuleId ? 'Update Rule' : 'Create Rule';
    }
}

async function deleteRule(ruleId) {
    try {
        await apiRequest(`/${ruleId}`, { method: 'DELETE' });
        setAlert('Rule deleted successfully.', 'success');
        rules = rules.filter(rule => rule.id !== ruleId);
        renderRules();
    } catch (error) {
        setAlert(parseErrorMessage(error), 'error');
    }
}

async function toggleRule(ruleId, enabled) {
    try {
        const updatedRule = await apiRequest(`/${ruleId}/toggle?enabled=${enabled}`, { method: 'PATCH' });
        setAlert(`Rule "${updatedRule.ruleName}" is now ${updatedRule.enabled ? 'enabled' : 'disabled'}.`, 'success');
        rules = rules.map(rule => rule.id === ruleId ? updatedRule : rule).sort(sortRules);
        renderRules();
    } catch (error) {
        setAlert(parseErrorMessage(error), 'error');
    }
}

async function inferRulesFromModel() {
    elements.inferBtn.disabled = true;
    const originalText = elements.inferBtn.textContent;
    elements.inferBtn.textContent = 'Inferring...';
    try {
        const response = await fetch(`${CONFIG.RULES_API_BASE}/infer/run?sampleSize=50`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        });
        if (!response.ok) {
            throw new Error(`Inference failed (${response.status})`);
        }
        const result = await response.json();
        const generatedCount = Array.isArray(result.generatedRules) ? result.generatedRules.length : 0;
        setAlert(result.message || `Model generated ${generatedCount} rules.`, 'success');
        await loadRules();
    } catch (error) {
        console.error('Inference failed:', error);
        setAlert(`Rule inference failed: ${error.message}`, 'error');
    } finally {
        elements.inferBtn.disabled = false;
        elements.inferBtn.textContent = originalText;
    }
}

function collectFormData() {
    const {
        ruleName,
        ruleType,
        thresholdValue,
        operator,
        priority,
        importance,
        description,
        failureMessage,
        enabled
    } = elements.inputs;

    if (!ruleName.value.trim() || !ruleType.value || !operator.value || !description.value.trim()) {
        setAlert('Please fill in all required fields.', 'error');
        return null;
    }

    const threshold = parseFloat(thresholdValue.value);
    if (Number.isNaN(threshold)) {
        setAlert('Threshold value must be a valid number.', 'error');
        return null;
    }

    const priorityValue = parseInt(priority.value, 10);
    if (Number.isNaN(priorityValue) || priorityValue < 1) {
        setAlert('Priority must be a positive whole number.', 'error');
        return null;
    }

    return {
        ruleName: ruleName.value.trim(),
        ruleType: ruleType.value,
        description: description.value.trim(),
        thresholdValue: threshold,
        operator: operator.value,
        priority: priorityValue,
        importance: importance.value,
        enabled: enabled.checked,
        failureMessage: failureMessage.value.trim() || null
    };
}

function populateForm(rule) {
    editingRuleId = rule.id;
    elements.formTitle.textContent = `Edit Rule: ${rule.ruleName}`;
    elements.submitBtn.textContent = 'Update Rule';
    elements.cancelEditBtn.style.display = 'inline-flex';

    elements.inputs.ruleId.value = rule.id;
    elements.inputs.ruleName.value = rule.ruleName || '';
    elements.inputs.ruleType.value = rule.ruleType || '';
    elements.inputs.thresholdValue.value = rule.thresholdValue ?? '';
    elements.inputs.operator.value = rule.operator || '';
    elements.inputs.priority.value = rule.priority ?? 1;
    elements.inputs.importance.value = rule.importance || 'CRITICAL';
    elements.inputs.description.value = rule.description || '';
    elements.inputs.failureMessage.value = rule.failureMessage || '';
    elements.inputs.enabled.checked = Boolean(rule.enabled);

    window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
}

function resetForm() {
    editingRuleId = null;
    elements.form.reset();
    elements.inputs.priority.value = 1;
    elements.inputs.enabled.checked = true;
    elements.formTitle.textContent = 'Create New Rule';
    elements.submitBtn.textContent = 'Create Rule';
    elements.cancelEditBtn.style.display = 'none';
    elements.formAlert.style.display = 'none';
    elements.formAlert.textContent = '';
}

function setAlert(message, type = 'info') {
    if (!message) {
        elements.formAlert.style.display = 'none';
        elements.formAlert.textContent = '';
        elements.formAlert.className = 'alert';
        return;
    }

    elements.formAlert.textContent = message;
    elements.formAlert.className = `alert alert-${type}`;
    elements.formAlert.style.display = 'block';
}

function parseErrorMessage(error) {
    if (!error) return 'An unknown error occurred.';

    if (typeof error === 'string') return error;

    if (error.message) {
        try {
            const parsed = JSON.parse(error.message);
            if (parsed && parsed.message) {
                return parsed.message;
            }
        } catch (parseError) {
            // Ignore JSON parse errors; we'll use the message as-is
        }
        return error.message;
    }

    return 'An unexpected error occurred.';
}

function escapeHtml(value) {
    if (value === null || value === undefined) return '';
    return value
        .toString()
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function formatThreshold(value) {
    if (value === null || value === undefined) {
        return '-';
    }
    const numeric = Number(value);
    if (Number.isNaN(numeric)) {
        return escapeHtml(value);
    }
    return numeric % 1 === 0 ? numeric.toString() : numeric.toFixed(2);
}

function formatDate(value) {
    if (!value) return '-';
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return escapeHtml(value);
    }
    return `${date.toLocaleDateString()} ${date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`;
}

function formatImportanceClass(importance) {
    switch ((importance || '').toUpperCase()) {
        case 'CRITICAL':
            return 'critical';
        case 'HIGH':
            return 'high';
        case 'MEDIUM':
            return 'medium';
        case 'LOW':
            return 'low';
        default:
            return '';
    }
}

function formatConfidence(value) {
    if (value === null || value === undefined) {
        return '—';
    }
    const numeric = Number(value);
    if (Number.isNaN(numeric)) {
        return escapeHtml(value);
    }
    if (numeric <= 1) {
        return `${(numeric * 100).toFixed(0)}%`;
    }
    return `${numeric.toFixed(0)}%`;
}

// Kick off
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
} else {
    init();
}
