Building a "Pet Supplies Trading and Cooking Practice Management Platform Backend API Service" that provides resource domain capabilities for four user roles: platform administrators, merchant operators, regular buyers, and reviewers. 

**Authentication and Session Domain** 
Supports username and password login, with passwords requiring at least 8 characters, including both numbers and letters. Accounts are locked for 15 minutes after 5 consecutive failed login attempts. 

**Product and Category Domain** 
Supports SPU/SKU creation, querying, updating, listing/delisting, and batch import/export. Provides configuration for categories, brands, and attribute specifications (e.g., size, age group, flavor). Includes inventory alerts with configurable thresholds (default trigger when stock ≤ 10). 

**Instant Messaging Domain** 
Supports WebSocket-based session establishment, message sending/recall, read/unread status, and anti-spam features (e.g., duplicate text within the same session folded within 10 seconds, images limited to JPG/PNG formats ≤ 2MB with local fingerprint deduplication). 

**Notification Domain** 
Provides only internal message distribution and subscription query capabilities. System events cover order status updates, review results, and report handling, with delivery and read timestamps recorded. 

The backend is built as a monolithic Spring Boot application (no microservices) with local MySQL storage. Data models are organized by resource domains: 
- Product, SKU, Category, Brand, Attribute, Inventory Log 
- Session, Message, Notification 
- Report Metrics, Indicator Definitions, Achievement Archives, Attachment Versions, Audit Logs 

Key constraints include unique indexes for product codes and SKU barcodes, category hierarchy depth ≤ 4, and a default message retention period of 180 days. 

**Reporting Center** 
Manages indicator definitions, aggregates data by organization/time/business dimensions, supports custom reports and drill-down queries, and enables export and scheduled generation (default daily at 2:00 AM). Access is role-based with audit trails. 

**Cooking Process Assistance** 
Modeled as "Process-Step-Timer-Reminder," supporting multiple parallel timers, node reminders, step completion, and checkpoint resumption (progress saved every 30 seconds or on step changes). Technique cards are configurable with text and tags. 

**Practice Achievements Domain** 
Centralizes management of achievement reports and attachments with version control (version numbers increment for the same achievement and cannot be rolled back). Key fields are stored structurally (title, period, responsible person, conclusion are mandatory) and support template exports for completion certificates/assessment forms. 

**Security and Compliance** 
- TLS for data transmission 
- Encryption of sensitive fields (e.g., phone numbers, ID numbers) in the database and log desensitization 
- Dual approval required for permission changes and critical operations 
- Immutable audit logs (append-only) 
- Local backup and recovery strategy (full backup daily + incremental backup hourly, retained for 30 days) 

The entire system operates in an offline environment without internet access, with no external dependencies such as SMS, email, or third-party logins enabled.