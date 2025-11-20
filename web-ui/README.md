# Loan Origination Web UI

A smooth, modern web interface for loan applications with natural language processing (NLP) integration.

## Features

- **Natural Language Input**: Describe your loan needs in plain English
- **Real-time Processing**: Instant credit decisioning through MCP integration
- **Clean UI**: Minimal black and white theme with smooth animations
- **Detailed Results**: View decision reasoning, credit scores, and bureau reports
- **Responsive Design**: Works on desktop, tablet, and mobile
- **Business Rule Admin**: Manage decision rules with add/edit/delete/toggle controls

## Quick Start

### 1. Start the Backend Services

```bash
# From the project root
docker-compose up -d
./run-services.sh start
```

### 2. Serve the Web UI

Using Python:
```bash
cd web-ui
python3 -m http.server 3000
```

Using Node.js:
```bash
cd web-ui
npx http-server -p 3000
```

Using any static server:
```bash
cd web-ui
# Use your preferred static file server
```

### 3. Open in Browser

Navigate to: `http://localhost:3000`

## Usage

### Natural Language Examples

The UI accepts natural language input. Here are some examples:

**Example 1: Basic Loan Request**
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
Purpose: Business expansion
```

### What the System Extracts

The NLP parser automatically extracts:
- **SSN** (required): Social Security Number
- **Loan Amount** (required): Requested loan amount
- **Name**: First and last name (optional, defaults to "Applicant User")
- **Purpose**: Loan purpose (optional, defaults to "General Purpose")
- **Annual Income**: Your yearly income (optional)
- **Total Debt**: Existing debt (optional)
- **Monthly Cashflow**: Available cashflow (optional)
- **Applicant Age**: Detected from phrases like "I am 35 years old" (optional)

## Rule Configuration Admin

Empower business users to manage decision rules without code:

1. Start the backend services and serve the web UI (see Quick Start above).
2. Open http://localhost:3000/admin.html (or click **Manage Rules** in the main UI header).
3. Review existing rules: see rule name, type, thresholds, operator, priority, importance, status, and timestamps.
4. Create or edit rules using the form (unique name, type, operator, thresholds, priority, importance, failure message, enabled flag). **Supported types:** `CREDIT_SCORE`, `LOAN_AMOUNT`, `BUREAU_RESPONSE`, `AGE_LIMIT`.
5. Toggle rules on/off instantly or delete outdated rules. Changes take effect immediately in the Decision Engine.

> Note: Rules are validated by the backend. Duplicate names or invalid payloads will return helpful error messages in the admin UI.

## Architecture

```
┌─────────────┐
│   Web UI    │ (Port 3000)
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ API Gateway │ (Port 8080)
└──────┬──────┘
       │
       ├──► Orchestrator (8081)
       └──► Decision Engine (8082)
            └──► LLM/Rules Processing
```

## Configuration

Edit `app.js` to change API endpoints:

```javascript
const CONFIG = {
    API_BASE_URL: 'http://localhost:8080/api',
    MCP_ENDPOINT: 'http://localhost:8086/mcp',
    LLM_ENDPOINT: 'http://localhost:8082/api/llm'
};
```

## Features

### 1. Chat Interface
- Natural language input
- Conversation-style interaction
- Real-time status updates

### 2. Decision Results
- Decision status (APPROVED/REJECTED)
- Credit score
- Request ID for tracking
- Detailed metrics

### 3. Bureau Reports
- Experian credit report
- Equifax credit report
- Timestamps and status

### 4. Decision Reasoning
- Rule-by-rule evaluation
- Pass/fail status for each rule
- Detailed explanations
- Threshold comparisons

## Theme

The UI uses a minimalist black and white theme:
- **Primary**: Black (#000000)
- **Background**: White (#FFFFFF)
- **Secondary**: Light Gray (#F8F9FA)
- **Success**: Green (#10B981)
- **Error**: Red (#EF4444)

## Responsive Design

The UI is fully responsive and works on:
- Desktop (1400px+)
- Tablet (768px - 1400px)
- Mobile (< 768px)

## Browser Support

- Chrome/Edge (latest)
- Firefox (latest)
- Safari (latest)
- Mobile browsers

## Customization

### Change Theme

Edit `styles.css`:

```css
:root {
    --color-bg: #ffffff;
    --color-text: #000000;
    --color-primary: #000000;
    /* Modify other colors as needed */
}
```

### Add New Features

1. **Add UI Elements**: Edit `index.html`
2. **Add Styles**: Edit `styles.css`
3. **Add Logic**: Edit `app.js`

## Development

### File Structure

```
web-ui/
├── index.html      # Main applicant interface
├── admin.html      # Business rule configuration portal
├── styles.css      # Shared styling (app + admin)
├── app.js          # Applicant chat and decision logic
├── admin.js        # Rule configuration interactions
└── README.md       # This file
```

### NLP Parser

The current NLP parser uses regex patterns. For production, consider:
- Integrating with the existing LLM endpoint
- Using OpenAI API for parsing
- Implementing more sophisticated entity extraction

### MCP Integration

To enable full MCP orchestration:

1. Set up MCP server (see `MCP_ORCHESTRATION_GUIDE.md`)
2. Update `CONFIG.MCP_ENDPOINT` in `app.js`
3. Implement MCP tool calls in `submitLoanApplication()`

## Production Deployment

### 1. Build for Production

Minify assets:
```bash
# Minify CSS
npx clean-css-cli -o styles.min.css styles.css

# Minify JavaScript
npx terser app.js -o app.min.js

# Update index.html to use minified files
```

### 2. Deploy Options

**Option A: Static Hosting**
- Netlify: `netlify deploy --dir=web-ui`
- Vercel: `vercel --cwd web-ui`
- GitHub Pages
- AWS S3 + CloudFront

**Option B: Docker**
```dockerfile
FROM nginx:alpine
COPY web-ui /usr/share/nginx/html
EXPOSE 80
```

**Option C: Spring Boot**
Add web-ui as static resources in a Spring Boot module

### 3. CORS Configuration

Ensure backend services allow CORS:

```yaml
# orchestrator/src/main/resources/application.yml
spring:
  web:
    cors:
      allowed-origins: "http://localhost:3000,https://yourdomain.com"
      allowed-methods: "GET,POST,PUT,DELETE"
```

## Troubleshooting

### CORS Errors
- Check backend CORS configuration
- Use a proxy for development
- Enable CORS in browser (dev only)

### API Connection Issues
- Verify backend services are running
- Check API_BASE_URL in app.js
- Inspect browser console for errors

### NLP Parsing Issues
- Provide more explicit information
- Follow the example formats
- Check browser console for parsing errors

## Future Enhancements

- [ ] Voice input for loan applications
- [ ] Document upload for income verification
- [ ] Real-time chat with AI assistant
- [ ] Multi-language support
- [ ] Dark mode toggle
- [ ] Progressive Web App (PWA)
- [ ] Offline support
- [ ] Analytics dashboard

## License

Part of the Loan Origination System - see main project README

