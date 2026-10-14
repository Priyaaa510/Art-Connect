# ArtConnect – First-Time Setup

This guide takes you from a fresh computer to a running ArtConnect with sample data.
It takes about 15–20 minutes, most of which is installing tools.

ArtConnect has three parts, and all three must be running:

| Part | Technology | Default address | Setting that changes it |
|------|------------|-----------------|-------------------------|
| Database | PostgreSQL | `localhost:8081` | `DB_URL` in `.env` |
| Backend (API) | Spring Boot (Java) | http://localhost:8083 | `PORT` in `.env` (+ `API_PROXY_TARGET` in `frontend/.env.local`) |
| Frontend (website) | React (Vite) | http://localhost:5173 ← open this one in your browser | Vite picks the next free port if 5173 is busy |

> PostgreSQL normally runs on port **5432**. This project's defaults assume it runs on **8081**. If yours uses 5432 (or any other port), just put that port in `DB_URL` in step 4.

Commands below are for **Windows (PowerShell)**. macOS/Linux differences are noted where they matter.

---

## 1. Install the tools (one time)

| Tool | Version | Download | Check it works |
|------|---------|----------|----------------|
| Java JDK | 17 or newer | https://adoptium.net | `java -version` |
| Node.js | 20.19+ or 22.12+ (LTS) | https://nodejs.org | `node -v` and `npm -v` |
| PostgreSQL | 14 or newer | https://www.postgresql.org/download | see step 2 |
| Git | any | https://git-scm.com | `git --version` |

You do **not** need to install Maven. The project includes a Maven wrapper (`mvnw.cmd` / `mvnw`) that downloads it automatically.

**PostgreSQL install tips (Windows):**
- The installer asks for a port. Use **8081** to match this project's defaults, or keep **5432** and put 5432 in `DB_URL` in step 4.
- The installer asks for a password for the `postgres` user. **Write it down**; you need it in step 4.
- You can untick *Stack Builder* at the end.

Open a **new** terminal after installing, so the new commands are found.

---

## 2. Get the code

    git clone <repository-url>
    cd ArtConnect

(If you received the project as a zip, unzip it and open a terminal in the `ArtConnect` folder.)

---

## 3. Create the database

Create an empty database named `artconnect` (`-p` is PostgreSQL's port; use 5432 if that's yours):

    psql -h localhost -p 8081 -U postgres -c "CREATE DATABASE artconnect"

Enter the `postgres` password from step 1 when asked.

> **`psql` is not recognized?** Use the full path instead (change `18` to your version):
>
>     & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -h localhost -p 8081 -U postgres -c "CREATE DATABASE artconnect"
>
> Or use **pgAdmin** (installed with PostgreSQL): right-click *Databases* → *Create* → *Database…* → name it `artconnect` → *Save*.

Now fill it with the tables and sample data. Pick **one** option:

- **Option A (automatic, recommended):** do nothing. The backend creates all tables and loads the sample data itself the first time it starts (step 5).
- **Option B (run the SQL yourself):** load the complete script, which creates every table and inserts all the sample data:

      psql -h localhost -p 8081 -U postgres -d artconnect -f database/artconnect.sql

  (Or in pgAdmin: select the `artconnect` database → *Query Tool* → open `database/artconnect.sql` → *Execute*.)
  The backend recognises the already-populated database when it starts and won't load the data twice.

Either way you end up with the same data. Run the script only on an **empty** database.

---

## 4. Configure your settings (`.env`)

Secrets are not stored in the code. Copy the example file and fill it in:

    Copy-Item .env.example .env

(macOS/Linux: `cp .env.example .env`)

Open `.env` in a text editor and set at least:

    DB_URL=jdbc:postgresql://localhost:8081/artconnect
    DB_USERNAME=postgres
    DB_PASSWORD=your-postgres-password

The number after `localhost:` in `DB_URL` is your PostgreSQL port. To run the backend on a port other than 8083, also add `PORT=<number>`, and tell the frontend in `frontend/.env.local` (see step 6).

**Cloudinary (image uploads):** artwork and profile-photo uploads are stored on [Cloudinary](https://cloudinary.com).
Create a free account, open the *Dashboard*, and copy the three values into `.env`:

    CLOUDINARY_CLOUD_NAME=...
    CLOUDINARY_API_KEY=...
    CLOUDINARY_API_SECRET=...

Everything else (browsing, buying, bidding, reviews, and the sample data) works without Cloudinary; only *uploading new images* needs it.

**Payments and invoices (optional):** out of the box everything runs in **demo mode**. *Add to cart* → *Cart* → *Checkout* asks for a shipping address and email, then opens a UPI-style demo checkout ("ArtConnect Pay": UPI QR/ID, cards, netbanking, wallets). No money moves. The invoice email is "sent" to the demo `outbox/` folder as an `.html` file. To send real invoice emails, see *Invoices by email* in [README.md](README.md). To try the real Razorpay checkout, create a free Razorpay account, generate **Test Mode** keys, and add:

    PAYMENT_PROVIDER=razorpay
    RAZORPAY_KEY_ID=rzp_test_...
    RAZORPAY_KEY_SECRET=...

Test keys never charge real money. See the *Payments* section of [README.md](README.md#-payments) for test UPI IDs and webhook setup.

> `.env` is ignored by git. Never commit it or share it publicly.

---

## 5. Start the backend

In a terminal, from the `ArtConnect` folder:

    .\mvnw.cmd spring-boot:run

(macOS/Linux: `./mvnw spring-boot:run`)

The first run downloads dependencies and takes a few minutes. It is ready when you see:

    Started ArtConnectApplication in ... seconds

On this first start (if you chose option A in step 3) it also:
- creates all the database tables, and
- loads the sample data (artists, artworks, auctions, reviews, exhibitions).

**Leave this terminal open**; closing it stops the backend.

Quick check: open http://localhost:8083/api/categories. You should see a list of categories.

---

## 6. Start the frontend

Open a **second** terminal:

    cd frontend
    npm install
    npm run dev

`npm install` is only needed the first time (and after dependencies change).
It is ready when you see `Local: http://localhost:5173/`.

The website sends its API requests to the backend at **http://localhost:8083**. If your backend runs on another port, create `frontend/.env.local` (copy `frontend/.env.example`) and set:

    API_PROXY_TARGET=http://localhost:<backend port>

then restart `npm run dev`.

*(Optional)* To make the **Contact us** form work, set `VITE_CONTACT_EMAIL` in the same `frontend/.env.local` to the address that should receive messages. The first message triggers a confirmation email from FormSubmit that must be clicked once. Without it, the Contact page shows a short "not available" note instead of the form.

**Leave this terminal open too.**

---

## 7. Open the app

Go to **http://localhost:5173** and log in with a demo account. **Every demo password is `password123`**.

| Username | Role | Try this |
|----------|------|----------|
| `meera_kapoor` | Artist | *Studio*: upload artwork, manage sales, create an exhibition |
| `arjun_rao`, `lena_fischer`, `kofi_mensah` | Artist | Same as above, different portfolios |
| `priya_sharma` | Collector | Buy artwork, bid in *Auctions*, write reviews, *My Collection* |
| `daniel_lee`, `aisha_khan` | Collector | Outbid each other on the live auctions |
| `admin` | Admin | Can edit or delete any artwork, review or exhibition |

Or click **Join** to register your own artist or collector account.

---

## Everyday use (after the first setup)

You only repeat steps 5 and 6. Make sure PostgreSQL is running, then:

| Terminal 1 (project folder) | Terminal 2 (`frontend` folder) |
|-----------------------------|--------------------------------|
| `.\mvnw.cmd spring-boot:run` | `npm run dev` |

Open http://localhost:5173. To stop a server, press **Ctrl + C** in its terminal.

---

## Useful commands

| Task | Command |
|------|---------|
| Run the backend tests (no database needed) | `.\mvnw.cmd test` |
| Build the frontend for production | `cd frontend` then `npm run build` (only builds files into `frontend/dist`; it does **not** start anything) |
| Reset the database to fresh sample data | Stop the backend, then `psql -U postgres -c "DROP DATABASE artconnect" -c "CREATE DATABASE artconnect"`, then start the backend again |
| Start **without** sample data | Add `FLYWAY_LOCATIONS=classpath:db/migration` to `.env` **before** the first start (use this in production) |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `password authentication failed for user "postgres"` | `DB_PASSWORD` in `.env` is wrong. |
| `database "artconnect" does not exist` | You skipped step 3. |
| `Connection to localhost:8081 refused` (or your DB port) | PostgreSQL isn't running, or `DB_URL` has the wrong port. Windows: press Win+R → `services.msc` → start **postgresql-x64-NN**. macOS (Homebrew): `brew services start postgresql`. |
| `Port 8083 was already in use` | Another backend is still running. Close its terminal, or find it with `netstat -ano \| findstr :8083` and stop it with `taskkill /PID <id> /F`. |
| `JAVA_HOME is not set` / `java` not found | Install the JDK (step 1) and open a new terminal. |
| Website loads but shows errors / no artworks | The backend isn't running, or `API_PROXY_TARGET` in `frontend/.env.local` doesn't match its port (default 8083). Start it (step 5) and refresh. |
| `npm install` fails | Check `node -v` meets the version in step 1, then delete `frontend/node_modules` and retry. |
| Image upload says it failed | Check the three `CLOUDINARY_*` values in `.env`, then restart the backend. |
| Backend won't start: `needs RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET` | You set `PAYMENT_PROVIDER=razorpay` without keys. Add the keys, or set `PAYMENT_PROVIDER=mock`. |
| "The payment service is unavailable" | The Razorpay keys are wrong, or the server can't reach api.razorpay.com. Check the keys and the backend log. |
| An artwork shows **Reserved** | A buyer started checkout and has 30 minutes to pay. If they don't, it returns to sale automatically. In your cart it shows as unavailable until then. |
| Changed `.env` but nothing happened | `.env` is read at startup. Restart the backend (Ctrl + C, then step 5 again). |

---

## Where things live

| Path | What it is |
|------|-----------|
| `frontend/` | React website (pages, components, styles) |
| `src/main/java/…` | Spring Boot API (controllers, services, entities) |
| `src/main/resources/db/migration/` | Database schema (SQL, run automatically by Flyway) |
| `src/main/resources/db/sample/` | Sample data (SQL, run automatically on a fresh database) |
| `src/main/resources/static/images/samples/` | Images for the sample artworks |
| `.env` | Your local settings and secrets (not in git) |

See [README.md](README.md) for features, the REST API reference, and deployment.
