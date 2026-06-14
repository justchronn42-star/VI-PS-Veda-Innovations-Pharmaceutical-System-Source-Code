# VI-PS Pharma — POS & Inventory Management System

A desktop Point-of-Sale and pharmaceutical inventory management system built with **JavaFX**, **SQLite**, and **JasperReports**. Includes role-based access control, audit logging, custom branding, and PDF receipt/report generation through an XML-driven Jasper pipeline.

---

## ✨ Features

- **Point of Sale** — multi-item cart, stock deduction, PDF receipt printing
- **Inventory Management** — full CRUD on medicines with low-stock highlighting
- **Sales History** — view all past transactions, reprint any receipt
- **Audit Logs** — every create/update/delete action is logged with the acting user and timestamp
- **User Management** — admin-only screen to create, edit, deactivate, and delete user accounts
- **Role-Based Access Control** — `ADMIN`, `PHARMACIST`, `CASHIER` roles gate navigation and actions
- **Custom Branding** — drop in `logo.png` and `login_bg.png` to replace the default pill emoji and login background with your own pharmacy's branding
- **PDF Reports** — receipts and stock reports generated via JasperReports `.jrxml` templates fed by XML data files, with Unicode font support (₱ symbol renders correctly)

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
- **JavaFX 21.0.2** (Controls + FXML), pulled via Maven for development
- **SQLite** via `sqlite-jdbc 3.45.1.0`
- **JasperReports 6.21.0** + `jasperreports-fonts` (Unicode/₱ symbol support) + iText 2.1.7 (PDF export)
- **Jaxen 2.0.0** (XPath engine for `JRXmlDataSource`)
- **Maven** build

---

## 📋 Prerequisites

- **Eclipse IDE for Java Developers** (or Eclipse IDE for Enterprise Java/Web Developers, which includes m2e by default)
- **JDK 17** or later — [Adoptium Temurin](https://adoptium.net/) recommended
- **e(fx)clipse** plugin (installed via Eclipse Marketplace — see [Eclipse Setup](#-eclipse-setup))

Verify your Java version:
```bash
java -version
```

---

## 🚀 Getting Started

This project is set up to run via **Eclipse** (see [Eclipse Setup](#-eclipse-setup) below). It includes `.project` and `.classpath` files preconfigured for Maven (m2e) and JavaFX (e(fx)clipse).

### 1. Clone the repository
```bash
git clone https://github.com/<your-username>/vips-pharma.git
cd vips-pharma
```

Then follow the Eclipse setup instructions below to import, configure, and run the project.

---

## 🧩 Eclipse Setup

This project includes Eclipse project files (`.project`, `.classpath`) preconfigured for **Java 17**, **Maven (m2e)**, and **JavaFX (e(fx)clipse)**.

### 1. Install required plugins

From **Help → Eclipse Marketplace**, install:

- **m2e** — Maven integration (bundled with most Eclipse distributions by default)
- **e(fx)clipse** — search for "efxclipse" and install the JavaFX tooling

> e(fx)clipse provides the `JAVAFX_CONTAINER` classpath entry this project depends on. Without it, you'll see "Unbound classpath container" errors on the JavaFX libraries.

### 2. Import the project

1. **File → Import → Existing Maven Projects**
2. Browse to the cloned repository root (where `pom.xml` lives)
3. Select the project and click **Finish**

Eclipse will read `.project` and `.classpath`, apply the m2e Maven nature, and resolve dependencies via `pom.xml`.

### 3. Set the JRE

Ensure a **JavaSE-17** JRE is configured:

- **Window → Preferences → Java → Installed JREs** — add a JDK 17+ if none is listed
- Right-click the project → **Build Path → Configure Build Path → Libraries** — confirm the JRE System Library shows `JavaSE-17`

### 4. Run the application

Right-click `MainApp.java` → **Run As → Java Application**.

If you get *"Error: JavaFX runtime components are missing"* when running this way, e(fx)clipse isn't installed or the `JAVAFX_CONTAINER` isn't resolving — reinstall the e(fx)clipse plugin and clean/rebuild the project (**Project → Clean...**).

---

## 🔑 Default Login

On first launch, the database (`vips_pharma.db`) is created automatically in the working directory and seeded with a default administrator account:

| Username | Password | Role |
|---|---|---|
| `admin` | `admin123` | ADMIN |

**Change this password immediately** via **User Management → Edit** after your first login.

---

## 🎨 Custom Branding

The app looks for branding assets in the working directory (next to the JAR / executable) at startup:

| File | Purpose | Where it appears |
|---|---|---|
| `logo.png` (or `.jpg`/`.jpeg`/`.gif`) | Replaces the 💊 pill emoji | Login screen header, sidebar, and window/taskbar icon |
| `login_bg.png` (or `.jpg`/`.jpeg`) | Background image for the login screen's right-hand panel | Login screen only |

If these files are absent, the app falls back to the default pill emoji and a CSS gradient background — nothing breaks.

`login_bg.png` is also checked in `~/.vips_pharma/` and `<working dir>/images/`, in that order, before falling back to the bundled classpath default.

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

The `jasperreports-fonts` dependency bundles DejaVu Sans/Serif, which include Unicode glyphs needed to render the ₱ (Philippine peso) symbol correctly in generated PDFs.

---

## 📂 Project Structure

```
vips-pharma/
├── pom.xml
├── .project / .classpath        (Eclipse + m2e + e(fx)clipse config)
├── logo.png                      (optional — your branding)
├── login_bg.png                  (optional — your branding)
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

## 📦 Building the Portable Distribution

The portable/end-user distribution (see its own README) does **not** rely on Eclipse or a live Maven setup. Instead it bundles:

- **`vips-pharma-fat.jar`** — a shaded fat JAR containing all dependencies except JavaFX
- **`javafx-sdk-26.0.1/`** — a full local JavaFX SDK (downloaded separately from [openjfx.io](https://gluonhq.com/products/javafx/)), providing native libraries for `--module-path`
- **`VI-PS.exe`** — a native Windows launcher wrapping the same launch logic as `run.bat` (built with a tool such as Launch4j or jpackage)
- **`run.bat`** — launches via:
  ```bat
  java --module-path javafx-sdk-26.0.1\lib ^
       --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base ^
       -jar target\vips-pharma-fat.jar
  ```

To produce the fat JAR from Eclipse, run a **Maven Build...** with goal `package shade:shade` (requires the `maven-shade-plugin` in `pom.xml`), then copy the resulting fat JAR into a folder alongside a downloaded JavaFX SDK matching your target Java version.
