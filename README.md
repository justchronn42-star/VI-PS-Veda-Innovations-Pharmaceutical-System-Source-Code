# VIPS Pharma — POS & Inventory Management System

A desktop Point-of-Sale and pharmaceutical inventory management system built with **JavaFX**, **SQLite**, and **JasperReports**. Includes role-based access control, audit logging, and PDF receipt/report generation through an XML-driven Jasper pipeline.

---

## ✨ Features

- **Point of Sale** — multi-item cart, stock deduction, PDF receipt printing
- **Inventory Management** — full CRUD on medicines with low-stock highlighting
- **Sales History** — view all past transactions, reprint any receipt
- **Audit Logs** — every create/update/delete action is logged with the acting user and timestamp
- **User Management** — admin-only screen to create, edit, deactivate, and delete user accounts
- **Role-Based Access Control** — `ADMIN`, `PHARMACIST`, `CASHIER` roles gate navigation and actions
- **Custom Branding** — drop in your own `logo.png` to replace the default pill emoji on the login screen and sidebar
- **PDF Reports** — receipts and stock reports generated via JasperReports `.jrxml` templates fed by XML data files

---

## 🏗️ Architecture

```
DB (SQLite) → DAO → Java Model → XML Data Writer → .jrxml Template → JasperReports → PDF
```

| Layer | Responsibility |
|---|---|
| `model/` | Plain JavaFX-bound data objects (`Medicine`, `Receipt`, `User`, `AuditLog`, `CartItem`, `Role`) |
| `dao/` | SQL access — `InventoryDAO`, `ReceiptDAO`, `UserDAO` |
| `controller/` | JavaFX FXML controllers — one per screen |
| `report/` | `XmlDataWriter` (Java objects → XML) and `ReportGenerator` (XML + `.jrxml` → PDF) |
| `util/` | `DatabaseUtil` (schema/connection), `SessionManager` (current user), `PasswordUtil` (hashing), `LogoManager` (custom branding) |
| `resources/fxml/` | UI layouts |
| `resources/css/` | Application stylesheet (purple & yellow theme) |
| `resources/jrxml/` | JasperReports templates (`receipt.jrxml`, `receipt_reprint.jrxml`, `stock_report.jrxml`) |

---

## 🔐 Roles & Permissions

| Role | POS | Inventory (view) | Inventory (edit/delete) | Sales History | Audit Logs | User Management |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| **ADMIN** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| **PHARMACIST** | ❌ | ✅ | ✅ | ✅ | ✅ | ❌ |
| **CASHIER** | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ |

---

## 🧰 Tech Stack

- **Java 17**
- **JavaFX 21.0.2** (Controls + FXML)
- **SQLite** via `sqlite-jdbc 3.45.1.0`
- **JasperReports 6.21.0** + iText 2.1.7 (PDF export)
- **Jaxen 2.0.0** (XPath engine for `JRXmlDataSource`)
- **Maven** build

---

## 📋 Prerequisites

- **JDK 17** or later — [Adoptium Temurin](https://adoptium.net/) recommended
- **Apache Maven 3.8+**
- No separate JavaFX SDK install needed — Maven pulls the required JARs

Verify your setup:
```bash
java -version
mvn -version
```

---

## 🚀 Building & Running from Source

### 1. Clone the repository
```bash
git clone https://github.com/<your-username>/vips-pharma.git
cd vips-pharma
```

### 2. Build the project
```bash
mvn clean package
```

This compiles the code, copies all runtime dependencies (including JavaFX platform JARs) into `target/lib/`, and produces a thin application JAR at `target/vips-pharma.jar`.

### 3. Run the application

**Windows:**
```cmd
run.bat
```

**Linux / macOS:**
```bash
chmod +x run.sh
./run.sh
```

> **Why a run script and not `java -jar`?**
> JavaFX ships its native libraries as separate platform-specific JARs. The run scripts pass `--module-path target/lib --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base` so the JVM can locate them. Running `java -jar target/vips-pharma.jar` directly will fail with *"JavaFX runtime components are missing"*.

### Alternative: run during development
```bash
mvn javafx:run
```
This uses the `javafx-maven-plugin`, which configures the module path automatically — no build artifact needed.

---

## 🔑 Default Login

On first launch, the database (`vips_pharma.db`) is created automatically and seeded with a default administrator account:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ADMIN |

**Change this password immediately** via **User Management → Edit** after your first login.

---

## 🎨 Custom Branding

To replace the default 💊 pill emoji logo with your own image:

1. Place a file named `logo.png` (or `.jpg`, `.jpeg`, `.gif`) in the same directory as `run.bat` / `run.sh`
2. Restart the application

The image will appear in both the login screen header and the sidebar, automatically scaled and styled to match.

---

## 🗄️ Database Schema

The SQLite database is created automatically at `vips_pharma.db` in the working directory on first run. Tables:

- **`inventory`** — medicine records (id, name, stock, price)
- **`receipts`** — completed sales (id, med_name, quantity, total_amount, date_time, processed_by)
- **`audit_logs`** — action history (id, action, username, date_time)
- **`users`** — accounts (id, username, password_hash, role, active)

Passwords are stored as salted SHA-256 hashes via `PasswordUtil`.

---

## 🖨️ Report Generation Pipeline

Reports are **not** built programmatically — they follow a strict data → XML → template pipeline:

1. A DAO fetches rows from SQLite
2. `XmlDataWriter` serializes those Java objects to an XML file under `reports/data/`
3. `ReportGenerator` compiles the matching `.jrxml` template and binds it to the XML via `JRXmlDataSource` (XPath-driven)
4. JasperReports fills the template and exports a PDF to `reports/`

| Output | Template | XML Writer Method |
|---|---|---|
| POS Receipt | `receipt.jrxml` | `writeReceiptXml()` |
| Receipt Reprint | `receipt_reprint.jrxml` | `writeReceiptReprintXml()` |
| Stock Report | `stock_report.jrxml` | `writeStockXml()` |

---

## 📂 Project Structure

```
vips-pharma/
├── pom.xml
├── run.bat
├── run.sh
└── src/main/
    ├── java/com/vips/pharma/
    │   ├── MainApp.java
    │   ├── controller/
    │   ├── dao/
    │   ├── model/
    │   ├── report/
    │   └── util/
    └── resources/
        ├── css/style.css
        ├── fxml/
        ├── images/
        └── jrxml/
```

---
