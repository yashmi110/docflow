# Postman Collection Guide

## Overview

This guide explains how to use the Docflow Postman collection to test all API endpoints.

## Setup

### 1. Import Collection

1. Open Postman
2. Click **Import** button
3. Select `docs/Docflow.postman_collection.json`
4. Collection will appear in your workspace

### 2. Configure Variables

The collection uses two variables:

| Variable | Default Value | Description |
|----------|---------------|-------------|
| `base_url` | http://localhost:8080 | API base URL |
| `jwt_token` | (empty) | JWT authentication token |

**To modify**:
1. Right-click collection → **Edit**
2. Go to **Variables** tab
3. Update `base_url` if needed
4. `jwt_token` is auto-populated on login

### 3. Start Application

```bash
cd C:\Users\yashmik\CascadeProjects\docflow
.\gradlew.bat bootRun
```

Wait for application to start on port 8080.

## Authentication

### Login Flow

The collection includes pre-configured login requests for all seed users:

1. **Login - Admin**
   - Email: `admin@docflow.com`
   - Password: `Password@123`
   - Auto-saves JWT token to `{{jwt_token}}`

2. **Login - Finance**
   - Email: `finance1@docflow.com`
   - Password: `Password@123`

3. **Login - Manager**
   - Email: `manager1@docflow.com`
   - Password: `Password@123`

4. **Login - Employee**
   - Email: `employee1@docflow.com`
   - Password: `Password@123`

### How It Works

Each login request has a **Test** script that automatically saves the JWT token:

```javascript
if (pm.response.code === 200) {
    var jsonData = pm.response.json();
    pm.collectionVariables.set("jwt_token", jsonData.token);
}
```

### Using Authentication

All authenticated endpoints use Bearer token authentication:

```
Authorization: Bearer {{jwt_token}}
```

This is configured at the collection level, so all requests inherit it automatically.

## Testing Workflows

### Workflow 1: Payable Invoice

**Scenario**: Create and process an incoming invoice

1. **Login as Finance**
   - Run: `Authentication > Login - Finance`
   - Verify: Token saved

2. **Create Invoice**
   - Run: `Invoices IN > Create Invoice IN`
   - Note the `id` in response (e.g., 5)

3. **Upload Supporting Document**
   - Run: `Document Files > Upload File`
   - Update URL: `/api/docs/5/files`
   - Select a PDF file

4. **Submit Invoice**
   - Run: `Invoices IN > Submit Invoice IN`
   - Update URL: `/api/invoices/in/5/submit`

5. **Approve Invoice**
   - Run: `Invoices IN > Approve Invoice IN`
   - Update URL: `/api/invoices/in/5/approve`

6. **Pay Invoice**
   - Run: `Invoices IN > Pay Invoice IN`
   - Update URL: `/api/invoices/in/5/pay`

7. **View Audit Trail**
   - Run: `Audit Logs > Get Audit Logs by Document`
   - Update URL: `/api/audit-logs/document/5`

### Workflow 2: Expense Claim → Reimbursement

**Scenario**: Employee submits claim, manager approves, finance pays

#### Part A: Employee Creates Claim

1. **Login as Employee**
   - Run: `Authentication > Login - Employee`

2. **Create Expense Claim**
   - Run: `Expense Claims > Create Expense Claim`
   - Note the `id` (e.g., 7)

3. **Upload Receipts**
   - Run: `Document Files > Upload File`
   - Update URL: `/api/docs/7/files`
   - Upload receipt files

4. **Submit Claim**
   - Run: `Expense Claims > Submit Expense Claim`
   - Update URL: `/api/claims/7/submit`

#### Part B: Manager Approves

5. **Login as Manager**
   - Run: `Authentication > Login - Manager`

6. **Approve Claim**
   - Run: `Expense Claims > Approve Expense Claim (Manager)`
   - Update URL: `/api/claims/7/approve`

#### Part C: Finance Creates Reimbursement

7. **Login as Finance**
   - Run: `Authentication > Login - Finance`

8. **Create Reimbursement**
   - Run: `Reimbursements > Create Reimbursement`
   - Update body: `"expenseClaimId": 7`
   - Note reimbursement `id` (e.g., 8)

9. **Approve Reimbursement**
   - Run: `Reimbursements > Approve Reimbursement`
   - Update URL: `/api/reimbursements/8/approve`

10. **Pay Reimbursement**
    - Run: `Reimbursements > Pay Reimbursement`
    - Update URL: `/api/reimbursements/8/pay`

### Workflow 3: Advanced Filtering

1. **Login as Finance**

2. **Filter Pending Invoices**
   - Run: `Invoices IN > Filter Invoices IN (Advanced)`
   - Modify filter criteria as needed:
     ```json
     {
       "status": "PENDING",
       "dateFrom": "2024-11-01",
       "dateTo": "2024-11-30",
       "amountMin": 100.00
     }
     ```

3. **Review Results**
   - Check `totalAmount` and `count` in response
   - Verify pagination info

## Testing Authorization

### Test 1: Manager Approval (Valid)

1. Login as Manager
2. Approve expense claim created by their direct report
3. Should succeed (200 OK)

### Test 2: Wrong Manager (Invalid)

1. Create claim as employee1 (reports to manager1)
2. Login as different manager
3. Try to approve claim
4. Should fail (403 Forbidden)

### Test 3: PO-Based Routing

1. Login as Finance
2. Create invoice linked to PO with assigned approver
3. Login as different finance user
4. Try to approve invoice
5. Should fail (403 Forbidden) - only PO approver can approve

### Test 4: FINANCE-Only Approval

1. Login as Manager
2. Try to approve outgoing invoice
3. Should fail (403 Forbidden) - only FINANCE can approve

## Common Tasks

### Update Request URLs

When testing with new document IDs, update the URL:

**Before**:
```
{{base_url}}/api/invoices/in/1/approve
```

**After** (for document ID 5):
```
{{base_url}}/api/invoices/in/5/approve
```

### Modify Request Bodies

Click on request → **Body** tab → Edit JSON

Example - Change invoice amount:
```json
{
  "vendorId": 1,
  "invoiceNo": "INV-NEW-001",
  "total": 5000.00  // Changed from 2200.00
}
```

### View Responses

After running request:
- **Body** tab: Response data
- **Headers** tab: Response headers
- **Test Results** tab: Script execution results

### Save Responses

Right-click response → **Save Response** → Choose format

## Environment Setup (Optional)

For multiple environments (dev, staging, prod):

1. Create environment: **Environments** → **+**
2. Add variables:
   ```
   base_url: http://localhost:8080
   jwt_token: (empty)
   ```
3. Select environment from dropdown
4. Variables will be scoped to that environment

## Troubleshooting

### Issue: 401 Unauthorized

**Cause**: JWT token expired or not set

**Solution**:
1. Run appropriate login request
2. Verify token saved: Collection → Variables → `jwt_token`
3. Token expires after 24 hours - re-login if needed

### Issue: 403 Forbidden

**Cause**: Insufficient permissions

**Solution**:
1. Check user role requirements in error message
2. Login with appropriate user (admin, finance, manager)
3. Verify authorization rules (e.g., manager hierarchy)

### Issue: 400 Bad Request

**Cause**: Invalid request data or business rule violation

**Solution**:
1. Check error message in response
2. Verify request body matches expected format
3. Check business rules (e.g., duplicate invoice number)

### Issue: 404 Not Found

**Cause**: Document doesn't exist

**Solution**:
1. Verify document ID in URL
2. Check if document was created successfully
3. Use List endpoint to find valid IDs

### Issue: Connection Refused

**Cause**: Application not running

**Solution**:
```bash
.\gradlew.bat bootRun
```

Wait for "Started DocflowApplication" message.

## Request Examples

### Create Invoice with PO

```json
POST /api/invoices/in
{
  "vendorId": 1,
  "poHeaderId": 1,  // Links to PO
  "invoiceNo": "INV-WITH-PO-001",
  "invoiceDate": "2024-11-15",
  "dueDate": "2024-12-15",
  "currency": "USD",
  "subtotal": 4500.00,
  "tax": 500.00,
  "total": 5000.00
}
```

### Filter with Multiple Criteria

```json
POST /api/invoices/in/filter
{
  "status": "PENDING",
  "vendorId": 1,
  "dateFrom": "2024-11-01",
  "dateTo": "2024-11-30",
  "amountMin": 1000.00,
  "amountMax": 10000.00,
  "currency": "USD",
  "invoiceNo": "ACME"  // Partial search
}
```

### Create Multi-Item Expense Claim

```json
POST /api/claims
{
  "claimDate": "2024-11-15",
  "currency": "USD",
  "items": [
    {
      "description": "Hotel - 2 nights",
      "date": "2024-11-14",
      "category": "Lodging",
      "amount": 300.00
    },
    {
      "description": "Meals",
      "date": "2024-11-14",
      "category": "Meals & Entertainment",
      "amount": 120.00
    },
    {
      "description": "Ground transportation",
      "date": "2024-11-14",
      "category": "Transportation",
      "amount": 80.00
    }
  ]
}
```

## Collection Structure

```
Docflow API
├── Authentication
│   ├── Login - Admin
│   ├── Login - Finance
│   ├── Login - Manager
│   ├── Login - Employee
│   └── Signup
├── Invoices IN (Payable)
│   ├── Create Invoice IN
│   ├── Get Invoice IN by ID
│   ├── List Invoices IN
│   ├── Filter Invoices IN (Advanced)
│   ├── Update Invoice IN
│   ├── Submit Invoice IN
│   ├── Approve Invoice IN
│   ├── Reject Invoice IN
│   └── Pay Invoice IN
├── Invoices OUT (Receivable)
│   ├── Create Invoice OUT
│   ├── Get Invoice OUT by ID
│   ├── List Invoices OUT
│   ├── Submit Invoice OUT
│   ├── Approve Invoice OUT
│   └── Record Payment (Inbound)
├── Expense Claims
│   ├── Create Expense Claim
│   ├── Get Expense Claim by ID
│   ├── List Expense Claims
│   ├── Submit Expense Claim
│   ├── Approve Expense Claim (Manager)
│   └── Reject Expense Claim
├── Reimbursements
│   ├── Create Reimbursement
│   ├── Get Reimbursement by ID
│   ├── List Reimbursements
│   ├── Approve Reimbursement
│   └── Pay Reimbursement
├── Document Files
│   ├── Upload File
│   ├── List Files
│   ├── Download File
│   └── Delete File
└── Audit Logs
    └── Get Audit Logs by Document
```

## Tips & Best Practices

1. **Always Login First**: Run appropriate login request before testing endpoints

2. **Use Variables**: Leverage `{{base_url}}` and `{{jwt_token}}` for flexibility

3. **Save Requests**: Duplicate and modify requests for different test scenarios

4. **Organize Tests**: Create folders for different workflows

5. **Use Environments**: Set up dev/staging/prod environments

6. **Test Scripts**: Add assertions to verify responses:
   ```javascript
   pm.test("Status is 200", function() {
       pm.response.to.have.status(200);
   });
   
   pm.test("Invoice created", function() {
       var jsonData = pm.response.json();
       pm.expect(jsonData.status).to.eql("DRAFT");
   });
   ```

7. **Collection Runner**: Run entire workflows automatically
   - Click collection → **Run**
   - Select requests to run
   - View results

## Next Steps

1. Import collection into Postman
2. Start application
3. Run login request
4. Test complete workflows
5. Experiment with different scenarios
6. Add custom test scripts
7. Create environment for staging/production

## Support

For issues or questions:
- Check application logs
- Review API documentation
- Verify seed data is loaded
- Check database state
