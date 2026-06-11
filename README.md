# VI-PS Veda-Innovations-Pharmaceutical-System Source Code Setup
A project by BSIT 2-1 Batch 2024-2028 PUP Taguig for the Object Oriented Programming


## Requirements

| Tool | Version | Download |
|---|---|---|
| Java JDK | 17 or higher | https://adoptium.net |
| Eclipse IDE | 2023+ | https://www.eclipse.org/downloads |
| Maven | Built into Eclipse | — |
| JavaFX SDK | 21 or 26 | https://gluonhq.com/products/javafx |

---

## Project Structure

```
vips_pharma_v3_modified/
├── src/
│   └── main/
│       ├── java/com/vips/pharma/
│       │   ├── controller/       ← JavaFX controllers
│       │   ├── dao/              ← Database access
│       │   ├── model/            ← Data models
│       │   ├── report/           ← JasperReports PDF generation
│       │   ├── util/             ← SessionManager, DatabaseUtil
│       │   └── MainApp.java      ← Entry point
│       └── resources/
│           ├── fxml/             ← UI layout files
│           ├── jrxml/            ← JasperReports templates
│           └── css/              ← Stylesheets
├── logo.png                      ← Logo shown on PDF reports
├── login_bg.png                  ← Login screen background
└── pom.xml                       ← Maven dependencies
```

---

## Setup in Eclipse

**Step 1 — Import the project**
1. Open Eclipse
2. File → **Import** → Maven → **Existing Maven Projects**
3. Browse to the `vips_pharma_v3_modified` folder
4. Click **Finish**

**Step 2 — Download dependencies**
Eclipse will automatically download all Maven dependencies.
Wait for the progress bar at the bottom to finish.

If it doesn't auto-download:
- Right-click project → **Maven** → **Update Project** → OK

**Step 3 — Add JavaFX to the build path**
1. Download JavaFX SDK from https://gluonhq.com/products/javafx (version 21, Windows, SDK)
2. Extract to somewhere like `C:\javafx-sdk`
3. Right-click project → **Build Path** → **Configure Build Path**
4. Libraries tab → **Add External JARs**
5. Navigate to `C:\javafx-sdk\lib` and select all `.jar` files
6. Click **Apply and Close**

**Step 4 — Run the application**
1. Right-click `MainApp.java` → **Run As** → **Java Application**

Or via Maven:
- Right-click project → **Run As** → **Maven build...**
- Goals: `javafx:run`
- Click **Run**

---

## Building the JAR

Right-click project → **Run As** → **Maven build...**
- Goals: `clean package`
- Click **Run**

Output files will be in `target/`:
- `vips-pharma.jar` — the application JAR
- `lib/` — all dependency JARs

---

## Default Login

| Username | Password | Role |
|---|---|---|
| admin | admin123 | Administrator |

> ⚠️ Change the default password after first login via User Management.

---

## Database

The app uses SQLite. The database file `vips_pharma.db` is created automatically in the project root folder on first run.

---

## Roles

| Role | Permissions |
|---|---|
| **Administrator** | Full access — POS, Inventory, Sales History, Audit Logs, User Management |
| **Cashier** | POS, view Inventory, Sales History |
| **Inventory Clerk** | Inventory CRUD, Sales History, Audit Logs, Stock Report |

---

## Notes

- Place `logo.png` in the project root to show it on PDF receipts
- PDF reports are saved to the `reports/` folder
- Data XML files used by JasperReports are saved to `data/`
