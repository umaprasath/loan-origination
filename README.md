# Loan Origination System - Microservices Architecture

This is a Spring Boot microservices implementation based on the provided architectural diagram. The system implements a loan origination and credit decisioning platform with API-led connectivity.

**🚀 Quick Start**: See [Quick Start](#quick-start) section below to get running in minutes!

## Architecture Overview

The system consists of the following microservices:

1. **API Gateway** (Port 8080) - Entry point for all client requests
2. **Orchestrator Service** (Port 8081) - Coordinates workflow and implements scatter-gather pattern
3. **Decision Engine Service** (Port 8082) - Makes credit decisions, uses Redis cache, persists to Decision DB
4. **Experian Connector** (Port 8083) - Integrates with Experian credit bureau API
5. **Equifax Connector** (Port 8084) - Integrates with Equifax credit bureau API
6. **Audit & Logging Service** (Port 8085) - Handles audit logging, persists to PostgreSQL, publishes to Kafka

## Infrastructure Components

- **Redis** - Credit cache for decision engine
- **PostgreSQL** - Audit database
- **Kafka** - Event bus for asynchronous communication
- **Zookeeper** - Required for Kafka
- **Ollama** (Optional) - Local LLM server for AI-powered decision making

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- **Optional for LLM**: OpenAI API key or Ollama (see [LLM-Powered Decision Making](#llm-powered-decision-making) section)

## Quick Start

Get the system up and running in minutes:

```bash
# 1. Start all infrastructure services (Redis, PostgreSQL, Kafka, Ollama)
docker-compose up -d

# 2. Pull Ollama model (if using LLM)
docker exec -it ollama ollama pull llama2

# 3. Build all services
mvn clean install

# 4. Start all microservices
./run-services.sh start

# 5. Start the Web UI (optional)
cd web-ui
python3 -m http.server 3000

# 6. Test the API
curl -X POST http://localhost:8080/api/credit/check \
  -H "Content-Type: application/json" \
  -d '{
    "ssn": "123-45-6789",
    "firstName": "John",
    "lastName": "Doe",
    "loanAmount": 50000,
    "loanPurpose": "Home Purchase",
    "annualIncome": 75000,
    "totalDebt": 15000,
    "monthlyCashflow": 3500,
    "applicantAge": 35
  }'
```

**That's it!** The system is now running. Access:
- **Web UI**: http://localhost:3000 (Natural language loan application interface)
- **Rule Admin UI**: http://localhost:3000/admin.html (No-code rule configuration)
- **API Gateway**: http://localhost:8080
- **Orchestrator Swagger UI**: http://localhost:8081/swagger-ui/index.html
- **Decision Engine Swagger UI**: http://localhost:8082/swagger-ui/index.html (includes LLM management)
- **H2 Console**: http://localhost:8082/h2-console (Decision Engine DB)

For detailed setup instructions, see [Getting Started](#getting-started) below.

## Getting Started

### 1. Start Infrastructure Services

```bash
docker-compose up -d
```

This will start:
- Redis on port 6379
- PostgreSQL on port 5432
- Kafka on port 9092
- Zookeeper on port 2181
- Ollama on port 11434 (if using LLM features)

**Note:** After starting Ollama in Docker, you need to pull a model:
```bash
# Pull a model into the Docker container
docker exec -it ollama ollama pull llama2
# or
docker exec -it ollama ollama pull mistral
```

### 2. Build the Project

```bash
mvn clean install
```

### 3. Run the Microservices

You can run each service individually:

```bash
# Terminal 1 - API Gateway
cd api-gateway
mvn spring-boot:run

# Terminal 2 - Orchestrator
cd orchestrator
mvn spring-boot:run

# Terminal 3 - Decision Engine
cd decision-engine
mvn spring-boot:run

# Terminal 4 - Audit Logging
cd audit-logging
mvn spring-boot:run

# Terminal 5 - Experian Connector
cd experian-connector
mvn spring-boot:run

# Terminal 6 - Equifax Connector
cd equifax-connector
mvn spring-boot:run
```

Or use the Maven wrapper script to run all services:

```bash
# Start all services
./run-services.sh start

# Check status of all services
./run-services.sh status

# View logs for a specific service
./run-services.sh logs api-gateway

# Stop all services
./run-services.sh stop

# Restart all services
./run-services.sh restart

# Build all services
./run-services.sh build
```

Alternatively, you can run each service individually:

```bash
mvn spring-boot:run -pl api-gateway &
mvn spring-boot:run -pl orchestrator &
mvn spring-boot:run -pl decision-engine &
mvn spring-boot:run -pl audit-logging &
mvn spring-boot:run -pl experian-connector &
mvn spring-boot:run -pl equifax-connector &
```

## Web UI - Natural Language Interface

### Access the Web UI

1. Start the web server:
   ```bash
   cd web-ui
   python3 -m http.server 3000
   # Or use: npx http-server -p 3000
   ```

2. Open in browser: http://localhost:3000

### Natural Language Examples

Type your loan needs naturally - the AI will parse and process your request:

**Example 1: Basic Request**
```
I need a $50,000 loan for a home purchase. 
My SSN is 123-45-6789, my name is John Doe.
```

**Example 2: Detailed Application**
```
I'd like to apply for a $75,000 loan. 
My SSN is 234-56-7890, first name Jane, last name Smith.
I earn $90,000 annually, have $15,000 in debt, 
and my monthly cashflow is $4,500.
```

**Example 3: Minimal Request**
```
SSN: 345-67-8901
Loan amount: $100,000
I make 80000 a year with 5000 debt
```

### Features

- **Natural Language Processing**: Describe your loan needs in plain English
- **Real-time Decisioning**: Instant credit evaluation with LLM and rules
- **Detailed Results**: View credit scores, bureau reports, and decision reasoning
- **Business Rule Admin**: No-code interface to add, edit, toggle, or delete decision rules
- **Smooth UI**: Clean black and white theme with animations
- **Responsive**: Works on desktop, tablet, and mobile

## API Usage

### Credit Check Request

```bash
curl -X POST http://localhost:8080/api/credit/check \
  -H "Content-Type: application/json" \
  -d '{
    "ssn": "123-45-6789",
    "firstName": "John",
    "lastName": "Doe",
    "loanAmount": 50000,
    "loanPurpose": "Home Purchase",
    "applicantAge": 35
  }'
```

### Response

```json
{
  "requestId": "uuid",
  "status": "APPROVED",
  "creditScore": 750.50,
  "loanAmount": 50000,
  "decisionReason": "Credit score and loan amount meet requirements",
  "timestamp": "2024-01-15T10:30:00",
  "experianResponse": {
    "bureauName": "EXPERIAN",
    "creditScore": 750,
    "status": "SUCCESS",
    "timestamp": "2024-01-15T10:30:00"
  },
  "equifaxResponse": {
    "bureauName": "EQUIFAX",
    "creditScore": 751,
    "status": "SUCCESS",
    "timestamp": "2024-01-15T10:30:00"
  }
}
```

## Decision Reasoning & Explainability

The system includes a comprehensive reasoning engine that explains **why** a loan decision was made. This is crucial for:
- **Regulatory Compliance**: Financial regulations require explainable AI/decision-making
- **Customer Transparency**: Applicants need to understand why their loan was approved or rejected
- **Audit & Debugging**: Helps identify issues in decision logic

### Reasoning Components

Each decision includes detailed reasoning with:

1. **Rule Evaluations**: Each decision rule is evaluated with:
   - Rule name and description
   - Pass/fail status
   - Actual value vs. threshold
   - Detailed explanation
   - Importance level (CRITICAL, HIGH, MEDIUM, LOW)

2. **Input Values**: All inputs used in the decision:
   - Loan amount requested
   - Credit bureau responses
   - Number of successful bureau responses

3. **Calculated Values**: Computed metrics:
   - Average credit score
   - Credit score range
   - Number of valid bureau responses

4. **Decision Path**: Step-by-step flow showing:
   - How bureau responses were processed
   - Which rules were evaluated
   - Why each rule passed or failed
   - Final decision summary

### Accessing Reasoning

**In API Response**: The `CreditResponse` includes a `reasoning` field with full details:
```json
{
  "requestId": "...",
  "status": "APPROVED",
  "reasoning": {
    "summary": "Loan APPROVED: All 3 critical rules passed...",
    "ruleEvaluations": [...],
    "inputs": {...},
    "calculated": {...},
    "decisionPath": "1. Received credit bureau responses..."
  }
}
```

**Dedicated Endpoint**: Get reasoning for any past decision:
```bash
GET /api/decision/reasoning/{requestId}
```

### Example Reasoning Output

```json
{
  "summary": "Loan APPROVED: All 3 critical rules passed. Credit score of 699.00 and loan amount of 50000 meet all requirements.",
  "ruleEvaluations": [
    {
      "ruleName": "MINIMUM_CREDIT_SCORE",
      "ruleDescription": "Credit score must be at least 650",
      "passed": true,
      "actualValue": "699.00",
      "threshold": "650",
      "operator": ">=",
      "explanation": "Credit score 699.00 meets the minimum threshold of 650.00",
      "importance": "CRITICAL"
    },
    {
      "ruleName": "MAXIMUM_LOAN_AMOUNT",
      "ruleDescription": "Loan amount must not exceed 1000000",
      "passed": true,
      "actualValue": "50000",
      "threshold": "1000000",
      "operator": "<=",
      "explanation": "Loan amount 50000.00 is within the maximum limit of 1000000.00",
      "importance": "CRITICAL"
    }
  ],
  "decisionPath": "1. Received credit bureau responses\n2. Calculated average credit score..."
}
```

## Architecture Patterns

### Scatter-Gather Pattern
The Orchestrator service implements parallel calls to Experian and Equifax connectors using reactive programming (WebFlux) with `Mono.zip()`.

### Caching
The Decision Engine uses Redis for caching credit decisions to improve performance.

### Event-Driven Architecture
Credit bureau responses are published to Kafka for asynchronous processing and monitoring.

### API Gateway Pattern
All client requests route through the API Gateway, which handles routing to the appropriate backend services.

### Explainable AI Pattern
The Decision Engine implements explainable decision-making by tracking and documenting all rules, inputs, and calculations used in each decision.

## Configurable Business Rules

The credit decisioning rules are **fully configurable** by business users through a REST API. No code changes are required to modify decision criteria.

### Rule Configuration API

All rule management endpoints are available at `/api/rules`:

**Get All Rules:**
```bash
GET /api/rules
```

**Get Rule by ID:**
```bash
GET /api/rules/{id}
```

**Create New Rule:**
```bash
POST /api/rules
Content-Type: application/json

{
  "ruleName": "MINIMUM_CREDIT_SCORE",
  "ruleType": "CREDIT_SCORE",
  "description": "Credit score must be at least 650",
  "thresholdValue": 650.00,
  "operator": ">=",
  "enabled": true,
  "priority": 1,
  "importance": "CRITICAL",
  "failureMessage": "Credit score below minimum threshold",
  "updatedBy": "admin@example.com"
}
```

**Update Existing Rule:**
```bash
PUT /api/rules/{id}
Content-Type: application/json

{
  "ruleName": "MINIMUM_CREDIT_SCORE",
  "thresholdValue": 700.00,  // Changed from 650 to 700
  ...
}
```

**Enable/Disable Rule:**
```bash
PATCH /api/rules/{id}/toggle?enabled=false
```

**Delete Rule:**
```bash
DELETE /api/rules/{id}
```

### Rule Types

The system supports three rule types:

1. **CREDIT_SCORE**: Evaluates against the average credit score
   - Example: `thresholdValue: 650, operator: ">="`

2. **LOAN_AMOUNT**: Evaluates against the requested loan amount
   - Example: `thresholdValue: 1000000, operator: "<="`

3. **BUREAU_RESPONSE**: Evaluates the number of successful bureau responses
   - Example: `thresholdValue: 1, operator: ">="` (at least one successful response)

4. **AGE_LIMIT**: Evaluates the applicant's age against minimum/maximum thresholds
   - Example: `thresholdValue: 18, operator: ">="` (must be at least 18 years old)

### Operators

Supported comparison operators:
- `>=` (greater than or equal)
- `<=` (less than or equal)
- `>` (greater than)
- `<` (less than)
- `==` (equal)

### Rule Properties

- **ruleName**: Unique identifier (e.g., `MINIMUM_CREDIT_SCORE`)
- **ruleType**: Type of rule (`CREDIT_SCORE`, `LOAN_AMOUNT`, `BUREAU_RESPONSE`)
- **description**: Human-readable description
- **thresholdValue**: The threshold value to compare against
- **operator**: Comparison operator
- **enabled**: Whether the rule is active
- **priority**: Execution order (lower number = higher priority)
- **importance**: `CRITICAL`, `HIGH`, `MEDIUM`, or `LOW`
- **failureMessage**: Custom message shown when rule fails

### Default Rules

The system initializes with three default rules on startup:

1. **MINIMUM_CREDIT_SCORE**: Credit score >= 650
2. **MAXIMUM_LOAN_AMOUNT**: Loan amount <= 1,000,000
3. **BUREAU_RESPONSE_VALIDATION**: At least 1 successful bureau response

### Rule Caching

Rules are cached in Redis for performance. Cache is automatically invalidated when rules are created, updated, or deleted.

### Example: Changing Minimum Credit Score

To change the minimum credit score from 650 to 700:

```bash
# 1. Get the rule ID
curl http://localhost:8082/api/rules | jq '.[] | select(.ruleName=="MINIMUM_CREDIT_SCORE")'

# 2. Update the threshold
curl -X PUT http://localhost:8082/api/rules/1 \
  -H "Content-Type: application/json" \
  -d '{
    "ruleName": "MINIMUM_CREDIT_SCORE",
    "ruleType": "CREDIT_SCORE",
    "description": "Credit score must be at least 700",
    "thresholdValue": 700.00,
    "operator": ">=",
    "enabled": true,
    "priority": 1,
    "importance": "CRITICAL",
    "failureMessage": "Credit score below minimum threshold of 700",
    "updatedBy": "business.user@example.com"
  }'
```

The change takes effect immediately for all new loan decisions!

## LLM-Powered Decision Making

The system supports **Large Language Model (LLM)** integration for intelligent loan decisioning. The LLM is trained with your configured business rules and can analyze complex scenarios including credit score, income, debt, and cashflow.

### How to Enable LLM-Based Decision Making

You can use either **OpenAI API** (cloud-based) or **Ollama** (local, free) for LLM decision making.

## Option A: Using Ollama (Local, Free, Recommended for Development)

**Benefits of Ollama:**
- ✅ **Free** - No API costs
- ✅ **Local** - Runs on your machine, no internet required
- ✅ **Private** - Data never leaves your machine
- ✅ **Fast** - No network latency
- ✅ **Offline** - Works without internet connection

### Using Ollama with Docker (Recommended)

Ollama is included in the Docker Compose setup. Simply:

#### Step 1: Start Ollama in Docker

```bash
# Start all infrastructure services including Ollama
docker-compose up -d

# Pull a model into the Docker container
docker exec -it ollama ollama pull llama2
# or for better quality
docker exec -it ollama ollama pull mistral
```

#### Step 2: Configure Environment Variables

```bash
# Enable LLM
export LLM_ENABLED=true

# Use Ollama as provider
export LLM_PROVIDER=ollama

# Use localhost when services run on host machine (default)
# Or use 'http://ollama:11434' if services run inside Docker
export OLLAMA_BASE_URL=http://localhost:11434

# Choose model (must match one you pulled)
export LLM_MODEL=llama2

# Choose decision mode: "rules", "llm", or "hybrid"
export DECISION_MODE=hybrid
```

**Important:** 
- If your Spring Boot services run **on the host machine** (default), use `http://localhost:11434` (exposed port)
- If your Spring Boot services run **inside Docker**, use `http://ollama:11434` (Docker service name)
- The default configuration uses `http://localhost:11434` for host-based services

#### Step 3: Restart Decision Engine

```bash
./run-services.sh restart decision-engine
```

#### Step 4: Verify Ollama Integration

Check logs:
```bash
./run-services.sh logs decision-engine | grep -i ollama
```

You should see:
```
Ollama client initialized with base URL: http://ollama:11434
LLM Decision Service initialized with Ollama, model: llama2
```

### Training Ollama with Risk Model

You can create a custom Ollama model trained specifically for loan risk assessment. This allows the LLM to have built-in knowledge of risk factors, decision criteria, and financial analysis patterns.

#### Step 1: Create a Modelfile

Create a file called `Modelfile` with your risk assessment rules and training data:

```bash
# Create Modelfile for loan risk assessment
cat > Modelfile << 'EOF'
FROM llama2

# System prompt for loan risk assessment
SYSTEM """
You are an expert loan risk assessment AI trained specifically for credit decisioning.

Your expertise includes:
- Credit score analysis and interpretation
- Debt-to-income ratio evaluation
- Cashflow analysis for loan affordability
- Risk factor identification
- Regulatory compliance in lending

Decision Criteria:
1. Credit Score: Minimum 650 for approval, higher scores preferred
2. Debt-to-Income Ratio: Should be below 43% for approval
3. Cashflow: Positive monthly cashflow required, minimum 3x loan payment
4. Loan Amount: Must be reasonable relative to income (typically < 4x annual income)
5. Credit History: Consider payment history, credit utilization, and recent inquiries

Risk Factors to Consider:
- High debt-to-income ratio (>43%)
- Negative cashflow
- Recent credit inquiries (potential over-leveraging)
- Low credit score (<650)
- Large loan amount relative to income

Always provide:
- Clear decision (APPROVED/REJECTED)
- Risk assessment score (1-10)
- Specific reasons for decision
- Recommendations for improvement if rejected
"""

# Training examples
TEMPLATE """{{ .System }}

User: {{ .Prompt }}

Assistant: {{ .Response }}"""

PARAMETER temperature 0.1
PARAMETER top_p 0.9
PARAMETER top_k 40
EOF
```

#### Step 2: Create Training Data (Optional but Recommended)

For better results, create a training dataset with examples:

```bash
# Create training data file
cat > training-data.jsonl << 'EOF'
{"input": "Credit score: 720, Income: $80,000, Debt: $10,000, Loan: $50,000, Cashflow: $4,000", "output": "APPROVED - Strong credit profile. Credit score 720 exceeds minimum. Debt-to-income ratio 12.5% is excellent. Positive cashflow of $4,000/month provides good buffer. Risk score: 2/10 (low risk)."}
{"input": "Credit score: 580, Income: $50,000, Debt: $30,000, Loan: $100,000, Cashflow: $500", "output": "REJECTED - Multiple risk factors. Credit score 580 below minimum threshold of 650. Debt-to-income ratio 60% exceeds maximum 43%. Low cashflow of $500/month insufficient for loan payments. Risk score: 9/10 (high risk)."}
{"input": "Credit score: 680, Income: $75,000, Debt: $20,000, Loan: $60,000, Cashflow: $3,500", "output": "APPROVED - Good credit profile. Credit score 680 meets requirements. Debt-to-income ratio 26.7% is acceptable. Positive cashflow provides adequate coverage. Risk score: 4/10 (moderate-low risk)."}
{"input": "Credit score: 700, Income: $100,000, Debt: $5,000, Loan: $200,000, Cashflow: $6,000", "output": "APPROVED - Excellent credit profile. High credit score 700. Very low debt-to-income ratio 5%. Strong cashflow. Loan amount 2x annual income is reasonable. Risk score: 1/10 (very low risk)."}
{"input": "Credit score: 640, Income: $40,000, Debt: $25,000, Loan: $80,000, Cashflow: -$200", "output": "REJECTED - Multiple concerns. Credit score 640 is marginal. Debt-to-income ratio 62.5% is too high. Negative cashflow indicates financial stress. Loan amount 2x annual income is high given other factors. Risk score: 8/10 (high risk)."}
EOF
```

#### Step 3: Create the Custom Model

**For Docker Ollama:**
```bash
# Copy Modelfile into container
docker cp Modelfile ollama:/tmp/Modelfile

# Create the custom model
docker exec -it ollama ollama create loan-risk-assessor -f /tmp/Modelfile

# Verify the model was created
docker exec ollama ollama list
```

**For Local Ollama:**
```bash
# Create the custom model
ollama create loan-risk-assessor -f Modelfile

# Verify the model was created
ollama list
```

#### Step 4: Use the Trained Model

Update your configuration to use the trained model:

```bash
# Set the model name
export LLM_MODEL=loan-risk-assessor

# Restart decision engine
./run-services.sh restart decision-engine
```

Or update `application.yml`:
```yaml
llm:
  enabled: true
  provider: ollama
  model: loan-risk-assessor  # Your trained model
```

#### Step 5: Fine-tune with More Data (Advanced)

For production use, you can fine-tune the model with historical loan data:

```bash
# Create a fine-tuning dataset from your historical decisions
# Format: JSONL with input/output pairs

# Fine-tune the model (requires Ollama with fine-tuning support)
docker exec -it ollama ollama train loan-risk-assessor -f training-data.jsonl
```

**Note:** Fine-tuning support in Ollama may vary by version. Check [Ollama documentation](https://github.com/ollama/ollama) for the latest fine-tuning capabilities.

#### Benefits of Trained Risk Model

- **Domain-Specific Knowledge**: Model understands loan risk factors
- **Consistent Decisions**: Trained on your specific criteria
- **Better Reasoning**: Provides risk scores and detailed analysis
- **Regulatory Compliance**: Built-in knowledge of lending regulations
- **Faster Decisions**: Less prompt engineering needed

#### Example: Using Trained Model

The trained model will automatically understand risk factors:

```json
{
  "creditScore": 720,
  "annualIncome": 80000,
  "totalDebt": 10000,
  "loanAmount": 50000,
  "monthlyCashflow": 4000
}
```

The model will analyze:
- Debt-to-income: 12.5% (excellent)
- Cashflow coverage: Strong
- Credit score: Above threshold
- Risk assessment: Low risk

### Using Ollama Locally (Alternative)

#### Step 1: Install Ollama

1. Download and install Ollama from [https://ollama.ai](https://ollama.ai)
2. Start Ollama service (it runs automatically after installation)
3. Pull a model (recommended: `llama2` or `mistral`):

```bash
# Pull a model (choose one)
ollama pull llama2        # ~3.8GB
ollama pull mistral       # ~4.1GB
ollama pull codellama     # ~3.8GB
ollama pull llama2:13b    # Larger, better quality (~7.3GB)
```

Verify Ollama is running:
```bash
curl http://localhost:11434/api/tags
```

**Recommended Models for Loan Decisioning:**
- `llama2` - Good balance of quality and speed (~3.8GB)
- `mistral` - Excellent reasoning capabilities (~4.1GB)
- `llama2:13b` - Higher quality, slower (~7.3GB)
- `codellama` - Good for structured outputs (~3.8GB)

**Note:** First run will download the model, which may take a few minutes depending on your internet speed.

#### Step 2: Configure Environment Variables

```bash
# Enable LLM
export LLM_ENABLED=true

# Use Ollama as provider
export LLM_PROVIDER=ollama

# Choose model (must match one you pulled)
export LLM_MODEL=llama2

# Use localhost for local Ollama installation
export OLLAMA_BASE_URL=http://localhost:11434

# Choose decision mode: "rules", "llm", or "hybrid"
export DECISION_MODE=hybrid
```

#### Step 3: Restart Decision Engine

```bash
./run-services.sh restart decision-engine
```

#### Step 4: Verify Ollama Integration

Check logs:
```bash
./run-services.sh logs decision-engine | grep -i ollama
```

You should see:
```
Ollama client initialized with base URL: http://localhost:11434
LLM Decision Service initialized with Ollama, model: llama2
```

## Option B: Using OpenAI API (Cloud-based)

#### Step 1: Get an OpenAI API Key

1. Sign up or log in to [OpenAI Platform](https://platform.openai.com/)
2. Navigate to [API Keys](https://platform.openai.com/api-keys)
3. Create a new secret key
4. Copy and securely store the API key

#### Step 2: Configure Environment Variables

Set the following environment variables:

```bash
# Enable LLM
export LLM_ENABLED=true

# Use OpenAI as provider
export LLM_PROVIDER=openai

# Your OpenAI API key
export OPENAI_API_KEY=sk-your-api-key-here

# Choose decision mode: "rules", "llm", or "hybrid"
export DECISION_MODE=hybrid

# Optional: Choose LLM model (default: gpt-4)
export LLM_MODEL=gpt-4
```

**Decision Mode Options:**
- `rules`: Traditional rule-based decisions only (default)
- `llm`: LLM-only decisions with rule context
- `hybrid`: Both rule-based and LLM must approve (most conservative)

#### Step 3: Update Application Configuration (Alternative)

Alternatively, you can update `decision-engine/src/main/resources/application.yml`:

**For Ollama (Docker):**
```yaml
llm:
  enabled: true
  provider: ollama
  model: llama2
  ollama:
    base-url: http://ollama:11434  # Use 'ollama' service name in Docker
decision:
  mode: hybrid
```

**For Ollama (Local):**
```yaml
llm:
  enabled: true
  provider: ollama
  model: llama2
  ollama:
    base-url: http://localhost:11434
decision:
  mode: hybrid
```

**For OpenAI:**
```yaml
llm:
  enabled: true
  provider: openai
  api-key: ${OPENAI_API_KEY:your-api-key-here}
  model: gpt-4
decision:
  mode: hybrid
```

**Note:** For security, prefer environment variables over hardcoding the API key in the config file.

#### Step 4: Restart the Decision Engine Service

```bash
# Stop the decision engine
./run-services.sh stop decision-engine

# Start it again with new configuration
./run-services.sh start decision-engine
```

Or restart all services:

```bash
./run-services.sh restart
```

#### Step 5: Verify LLM is Enabled

Check the decision engine logs:

**For Ollama:**
```bash
./run-services.sh logs decision-engine | grep -i "ollama\|llm"
```

You should see:
```
Ollama client initialized with base URL: http://localhost:11434
LLM Decision Service initialized with Ollama, model: llama2
Hybrid Decision Service initialized with mode: hybrid (LLM enabled: true)
```

**For OpenAI:**
```bash
./run-services.sh logs decision-engine | grep -i "llm"
```

You should see:
```
LLM Decision Service initialized with OpenAI, model: gpt-4
Hybrid Decision Service initialized with mode: hybrid (LLM enabled: true)
```# loan-origination
