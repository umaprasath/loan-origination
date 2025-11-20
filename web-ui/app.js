// Configuration
const CONFIG = {
    API_BASE_URL: 'http://localhost:8080/api',
    MCP_ENDPOINT: 'http://localhost:8086/mcp',
    LLM_ENDPOINT: 'http://localhost:8082/api/llm'
};

// State
let currentRequestId = null;
let isProcessing = false;

// DOM Elements
const elements = {
    chatInput: document.getElementById('chatInput'),
    chatMessages: document.getElementById('chatMessages'),
    submitBtn: document.getElementById('submitBtn'),
    submitText: document.getElementById('submitText'),
    submitLoader: document.getElementById('submitLoader'),
    clearBtn: document.getElementById('clearBtn'),
    resultPanel: document.getElementById('resultPanel'),
    closeResultBtn: document.getElementById('closeResultBtn'),
    statusDot: document.getElementById('statusDot'),
    statusText: document.getElementById('statusText'),
    
    // Result elements
    decisionStatus: document.getElementById('decisionStatus'),
    statusBadge: document.getElementById('statusBadge'),
    statusMessage: document.getElementById('statusMessage'),
    creditScore: document.getElementById('creditScore'),
    loanAmount: document.getElementById('loanAmount'),
    requestId: document.getElementById('requestId'),
    reasoningSection: document.getElementById('reasoningSection'),
    reasoningContent: document.getElementById('reasoningContent'),
    bureauSection: document.getElementById('bureauSection'),
    bureauGrid: document.getElementById('bureauGrid'),
    viewReasoningBtn: document.getElementById('viewReasoningBtn'),
    newApplicationBtn: document.getElementById('newApplicationBtn')
};

// Initialize
document.addEventListener('DOMContentLoaded', () => {
    setupEventListeners();
    checkSystemStatus();
});

// Event Listeners
function setupEventListeners() {
    elements.submitBtn.addEventListener('click', handleSubmit);
    elements.clearBtn.addEventListener('click', handleClear);
    elements.closeResultBtn.addEventListener('click', hideResultPanel);
    elements.viewReasoningBtn.addEventListener('click', toggleReasoning);
    elements.newApplicationBtn.addEventListener('click', handleNewApplication);
    
    elements.chatInput.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
            handleSubmit();
        }
    });
}

// Check System Status
async function checkSystemStatus() {
    try {
        const response = await fetch(`${CONFIG.LLM_ENDPOINT}/health`);
        const data = await response.json();
        
        if (data.status === 'UP') {
            elements.statusDot.style.background = '#10b981';
            elements.statusText.textContent = 'Online';
        } else {
            elements.statusDot.style.background = '#f59e0b';
            elements.statusText.textContent = 'Limited';
        }
    } catch (error) {
        elements.statusDot.style.background = '#ef4444';
        elements.statusText.textContent = 'Offline';
        console.error('Status check failed:', error);
    }
}

// Handle Submit
async function handleSubmit() {
    const input = elements.chatInput.value.trim();
    
    if (!input || isProcessing) return;
    
    isProcessing = true;
    setLoadingState(true);
    
    // Add user message
    addMessage(input, 'user');
    
    try {
        // Parse natural language input
        const parsedData = await parseNaturalLanguage(input);
        
        // Submit loan application
        const result = await submitLoanApplication(parsedData);
        
        // Show result
        displayResult(result);
        
        // Add assistant response
        addMessage(
            `I've processed your loan application. Your request ID is ${result.requestId}. ` +
            `The decision is: ${result.status}. You can view the details in the result panel.`,
            'assistant'
        );
        
    } catch (error) {
        console.error('Submission error:', error);
        addMessage(
            `I apologize, but there was an error processing your application: ${error.message}. ` +
            `Please try again or contact support if the problem persists.`,
            'assistant'
        );
    } finally {
        isProcessing = false;
        setLoadingState(false);
        elements.chatInput.value = '';
    }
}

// Parse Natural Language using LLM
async function parseNaturalLanguage(text) {
    addMessage('Let me parse your request...', 'assistant');
    
    // Enhanced regex patterns for better NLP parsing
    const patterns = {
        // SSN patterns - multiple attempts from most specific to most general
        ssnWithKeyword: /(?:ssn|social\s*security|ss\s*number|id|ssn\s*is|ssn\s*:|id\s*:)[\s:=]*([0-9]{3}[\s-]?[0-9]{2}[\s-]?[0-9]{4})/i,
        ssnFormatted: /\b([0-9]{3}[\s-][0-9]{2}[\s-][0-9]{4})\b/,
        ssnPlain: /\b([0-9]{9})\b/,
        
        // Name patterns
        firstName: /(?:first\s*name|fname|my\s*name\s*is)[\s:=]*([a-z]+)/i,
        lastName: /(?:last\s*name|lname|surname)[\s:=]*([a-z]+)/i,
        fullName: /(?:name|i'm|im|i\s*am)[\s:=]*([a-z]+)\s+([a-z]+)/i,
        
        // Loan amount - various formats
        loanAmount: /(?:\$|usd|\busd\b|amount|loan)[\s:=]*([0-9,]+)(?:\s*(?:dollars?|usd|\bloan\b))?/i,
        loanAmountWord: /([0-9,]+)[\s]*(?:dollar|usd)(?:\s+loan)?/i,
        loanAmountSimple: /(?:need|want|requesting|apply\s+for)[\s]+\$?([0-9,]+)/i,
        
        // Purpose patterns
        loanPurpose: /(?:for|purpose|reason)[\s:=]*([a-z\s]+?)(?:\.|$|,|my|i\s)/i,
        
        // Income patterns
        annualIncome: /(?:income|earn|salary|make|making)[\s:=]*\$?([0-9,]+)(?:\s*(?:annually|yearly|per\s*year|a\s*year|\/year))?/i,
        
        // Debt patterns
        totalDebt: /(?:debt|owe|owing)[\s:=]*\$?([0-9,]+)/i,
        
        // Cashflow patterns
        monthlyCashflow: /(?:cashflow|cash\s*flow|monthly)[\s:=]*\$?([0-9,]+)(?:\s*(?:per\s*month|monthly|\/month))?/i,
        
        // Age patterns
        age: /(?:age|i\s*am|i'm)[\s:=]*([0-9]{1,3})(?=\s*(?:years?|yrs?|yo|\b))/i
    };
    
    const data = {};
    
    // Extract SSN - try multiple patterns
    let ssnMatch = text.match(patterns.ssnWithKeyword);
    if (!ssnMatch) {
        ssnMatch = text.match(patterns.ssnFormatted);
    }
    if (!ssnMatch) {
        ssnMatch = text.match(patterns.ssnPlain);
    }
    
    if (ssnMatch) {
        const ssnDigits = ssnMatch[1].replace(/[^0-9]/g, '');
        if (ssnDigits.length === 9) {
            data.ssn = ssnDigits.replace(/(\d{3})(\d{2})(\d{4})/, '$1-$2-$3');
        }
    }
    
    // Extract names - try full name first
    const fullNameMatch = text.match(patterns.fullName);
    if (fullNameMatch) {
        data.firstName = capitalize(fullNameMatch[1]);
        data.lastName = capitalize(fullNameMatch[2]);
    } else {
        const firstNameMatch = text.match(patterns.firstName);
        const lastNameMatch = text.match(patterns.lastName);
        if (firstNameMatch) data.firstName = capitalize(firstNameMatch[1]);
        if (lastNameMatch) data.lastName = capitalize(lastNameMatch[1]);
    }
    
    // Extract loan amount - try multiple patterns
    let loanAmountMatch = text.match(patterns.loanAmount);
    if (!loanAmountMatch) {
        loanAmountMatch = text.match(patterns.loanAmountWord);
    }
    if (!loanAmountMatch) {
        loanAmountMatch = text.match(patterns.loanAmountSimple);
    }
    
    if (loanAmountMatch) {
        const amount = parseFloat(loanAmountMatch[1].replace(/,/g, ''));
        if (!isNaN(amount) && amount > 0) {
            data.loanAmount = amount;
        }
    }
    
    // Extract purpose
    const purposeMatch = text.match(patterns.loanPurpose);
    if (purposeMatch) {
        data.loanPurpose = capitalize(purposeMatch[1].trim());
    }
    
    // Extract income
    const incomeMatch = text.match(patterns.annualIncome);
    if (incomeMatch) {
        const income = parseFloat(incomeMatch[1].replace(/,/g, ''));
        if (!isNaN(income) && income > 0) {
            data.annualIncome = income;
        }
    }
    
    // Extract debt
    const debtMatch = text.match(patterns.totalDebt);
    if (debtMatch) {
        const debt = parseFloat(debtMatch[1].replace(/,/g, ''));
        if (!isNaN(debt) && debt >= 0) {
            data.totalDebt = debt;
        }
    }
    
    // Extract cashflow
    const cashflowMatch = text.match(patterns.monthlyCashflow);
    if (cashflowMatch) {
        const cashflow = parseFloat(cashflowMatch[1].replace(/,/g, ''));
        if (!isNaN(cashflow)) {
            data.monthlyCashflow = cashflow;
        }
    }
    
    // Extract age
    const ageMatch = text.match(patterns.age);
    if (ageMatch) {
        const age = parseInt(ageMatch[1], 10);
        if (!isNaN(age) && age > 0) {
            data.applicantAge = age;
        }
    }
    
    // Validate required fields
    const missingFields = [];
    if (!data.ssn) missingFields.push('SSN (e.g., 123-45-6789)');
    if (!data.loanAmount) missingFields.push('loan amount (e.g., $50,000)');
    
    if (missingFields.length > 0) {
        throw new Error(`Missing required information: ${missingFields.join(' and ')}. \n\nPlease include: "${missingFields.join(' and ')}" in your message.`);
    }
    
    // Set defaults for optional fields
    data.firstName = data.firstName || 'Applicant';
    data.lastName = data.lastName || 'User';
    data.loanPurpose = data.loanPurpose || 'General Purpose';
    
    // Show what was extracted
    const extracted = [];
    extracted.push(`SSN: ${data.ssn}`);
    extracted.push(`Amount: $${formatNumber(data.loanAmount)}`);
    if (data.firstName !== 'Applicant') extracted.push(`Name: ${data.firstName} ${data.lastName}`);
    if (data.annualIncome) extracted.push(`Income: $${formatNumber(data.annualIncome)}`);
    if (data.totalDebt) extracted.push(`Debt: $${formatNumber(data.totalDebt)}`);
    if (data.monthlyCashflow) extracted.push(`Cashflow: $${formatNumber(data.monthlyCashflow)}`);
    if (data.applicantAge) extracted.push(`Age: ${data.applicantAge}`);
    
    addMessage(`Great! I extracted: ${extracted.join(', ')}`, 'assistant');
    
    return data;
}

// Submit Loan Application
async function submitLoanApplication(data) {
    addMessage('Submitting your application to the credit decisioning system...', 'assistant');
    
    try {
        const response = await fetch(`${CONFIG.API_BASE_URL}/credit/check`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify(data)
        });
        
        if (!response.ok) {
            let errorMessage = 'Failed to submit application';
            try {
                const error = await response.json();
                errorMessage = error.message || errorMessage;
            } catch (e) {
                errorMessage = `Server returned ${response.status}: ${response.statusText}`;
            }
            throw new Error(errorMessage);
        }
        
        return await response.json();
    } catch (error) {
        if (error.message === 'Failed to fetch') {
            throw new Error(
                'Unable to connect to the server. Please ensure:\n' +
                '1. Backend services are running (./run-services.sh status)\n' +
                '2. API Gateway is accessible at http://localhost:8080\n' +
                '3. You have network connectivity'
            );
        }
        throw error;
    }
}

// State for current reasoning
let currentReasoning = null;

// Display Result
function displayResult(result) {
    currentRequestId = result.requestId;
    currentReasoning = result.reasoning; // Store reasoning from response
    
    // Update status
    const decision = result.status || result.decision;
    elements.statusBadge.textContent = decision;
    elements.statusBadge.className = `status-badge ${decision.toLowerCase()}`;
    elements.statusMessage.textContent = result.decisionReason || result.reason;
    
    // Update metrics
    elements.creditScore.textContent = result.creditScore ? result.creditScore.toFixed(0) : '---';
    elements.loanAmount.textContent = result.loanAmount ? `$${formatNumber(result.loanAmount)}` : '---';
    elements.requestId.textContent = result.requestId;
    
    // Display bureau responses
    if (result.experianResponse || result.equifaxResponse) {
        displayBureauResponses(result);
    }
    
    // If reasoning is available, enable the button
    if (currentReasoning && currentReasoning.ruleEvaluations && currentReasoning.ruleEvaluations.length > 0) {
        elements.viewReasoningBtn.disabled = false;
        elements.viewReasoningBtn.textContent = 'View Full Reasoning';
    } else {
        elements.viewReasoningBtn.disabled = true;
        elements.viewReasoningBtn.textContent = 'Reasoning Unavailable';
    }
    
    // Show result panel with emphasis
    elements.resultPanel.style.display = 'block';
    
    // Wait a moment for the panel to render, then scroll
    setTimeout(() => {
        elements.resultPanel.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, 100);
    
    // Add visual emphasis
    addMessage('✅ Decision complete! Check the result panel below for full details.', 'assistant');
}

// Display Bureau Responses
function displayBureauResponses(result) {
    elements.bureauSection.style.display = 'block';
    elements.bureauGrid.innerHTML = '';
    
    const bureaus = [
        { name: 'Experian', data: result.experianResponse },
        { name: 'Equifax', data: result.equifaxResponse }
    ];
    
    bureaus.forEach(bureau => {
        if (bureau.data) {
            const card = document.createElement('div');
            card.className = 'bureau-card';
            card.innerHTML = `
                <div class="bureau-header">
                    <div class="bureau-name">${bureau.data.bureauName || bureau.name}</div>
                    <span class="bureau-status ${bureau.data.status.toLowerCase()}">${bureau.data.status}</span>
                </div>
                <div class="bureau-score">${bureau.data.creditScore || 'N/A'}</div>
                <div class="bureau-timestamp">${formatDate(bureau.data.timestamp)}</div>
            `;
            elements.bureauGrid.appendChild(card);
        }
    });
}

// Toggle Reasoning
async function toggleReasoning() {
    if (elements.reasoningSection.style.display === 'block') {
        elements.reasoningSection.style.display = 'none';
        elements.viewReasoningBtn.textContent = 'View Full Reasoning';
    } else {
        // Display reasoning from stored data (already in response)
        if (!elements.reasoningContent.innerHTML && currentReasoning) {
            displayReasoning(currentReasoning);
        }
        elements.reasoningSection.style.display = 'block';
        elements.viewReasoningBtn.textContent = 'Hide Reasoning';
    }
}

// Display Reasoning
function displayReasoning(reasoning) {
    if (!reasoning.ruleEvaluations || reasoning.ruleEvaluations.length === 0) {
        elements.reasoningContent.innerHTML = '<p>No detailed reasoning available.</p>';
        return;
    }
    
    let html = '<div class="rule-evaluations">';
    
    reasoning.ruleEvaluations.forEach(rule => {
        html += `
            <div class="rule-evaluation ${rule.passed ? 'passed' : 'failed'}">
                <div class="rule-header">
                    <div class="rule-name">${rule.ruleName}</div>
                    <span class="rule-status ${rule.passed ? 'passed' : 'failed'}">
                        ${rule.passed ? 'Passed' : 'Failed'}
                    </span>
                </div>
                <div class="rule-description">${rule.ruleDescription}</div>
                <div class="rule-details">
                    ${rule.explanation || ''}
                    ${rule.actualValue && rule.threshold ? 
                        `<br><strong>Actual:</strong> ${rule.actualValue} | <strong>Required:</strong> ${rule.operator} ${rule.threshold}` : ''}
                </div>
            </div>
        `;
    });
    
    html += '</div>';
    elements.reasoningContent.innerHTML = html;
}

// Add Message
function addMessage(text, sender) {
    const message = document.createElement('div');
    message.className = `message ${sender}-message`;
    message.innerHTML = `
        <div class="message-icon">${sender === 'user' ? 'YOU' : 'AI'}</div>
        <div class="message-content">
            <p>${text}</p>
        </div>
    `;
    elements.chatMessages.appendChild(message);
    elements.chatMessages.scrollTop = elements.chatMessages.scrollHeight;
}

// Handle Clear
function handleClear() {
    elements.chatInput.value = '';
    elements.chatInput.focus();
}

// Handle New Application
function handleNewApplication() {
    hideResultPanel();
    elements.chatInput.value = '';
    elements.chatInput.focus();
    currentRequestId = null;
    currentReasoning = null;
}

// Hide Result Panel
function hideResultPanel() {
    elements.resultPanel.style.display = 'none';
    elements.reasoningSection.style.display = 'none';
    elements.reasoningContent.innerHTML = '';
}

// Set Loading State
function setLoadingState(loading) {
    elements.submitBtn.disabled = loading;
    elements.submitText.style.display = loading ? 'none' : 'inline';
    elements.submitLoader.classList.toggle('hidden', !loading);
}

// Utility Functions
function capitalize(str) {
    return str.charAt(0).toUpperCase() + str.slice(1).toLowerCase();
}

function formatNumber(num) {
    return new Intl.NumberFormat('en-US').format(num);
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    }).format(date);
}

