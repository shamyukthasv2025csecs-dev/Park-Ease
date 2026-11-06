# ParkEase – Smart Parking Lot Management System

ParkEase is a Java and MySQL parking application with a browser-based dashboard. Java serves the pages and handles parking rules, while HTML and CSS provide the interface.

## Project Structure

- `src/` – Java source files
- `web/index.html` and `web/styles.css` – browser interface
- `database/parkease.sql` – MySQL database creation script
- `config/db.properties` – database configuration for local MySQL setup
- `lib/` – JDBC driver jar

## Requirements

1. Java 21 or later
2. MySQL Server 8.x
3. MySQL Connector/J JDBC driver
4. VS Code or any Java IDE

## Installation on Windows

### 1. Install Java

Download and install Microsoft Build of OpenJDK 21 from:

- https://aka.ms/download-jdk

After installation, verify:

```bash
java -version
javac -version
```

### 2. Install MySQL

Install MySQL Server 8.x and MySQL Workbench from MySQL's official website or a package manager such as winget.

If MySQL is installed but the service is not running, start it manually from a terminal:

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' --datadir='C:\Users\<YourUser>\MySQLData' --console
```

If the server has not been initialized yet, run:

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' --initialize-insecure --datadir='C:\Users\<YourUser>\MySQLData' --console
```

Then create the database using the script:

```powershell
Get-Content '.\database\parkease.sql' | & 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' -u root
```

### 3. Download JDBC Driver

Download the MySQL Connector/J jar from Maven Central:

- https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar

Place the jar in `lib/`.

### 4. Configure Database Connection

Copy `config/db.properties.example` to `config/db.properties`, then update the username/password for your MySQL account:

```properties
db.url=jdbc:mysql://localhost:3306/parkease_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=
```

If you use a password-protected MySQL user, replace `db.user` and `db.password` accordingly. The local `config/db.properties` file is ignored by Git so your password is not committed.

## Compile

From the project root:

```bash
javac -cp "lib/mysql-connector-j-8.4.0.jar" -d out src/*.java
```

## Run the website

From the project root in PowerShell:

```powershell
javac -cp "lib\mysql-connector-j-8.4.0.jar" -d out src\*.java
java -cp "out;lib\mysql-connector-j-8.4.0.jar" WebServer
```

Keep the terminal open, then visit [http://localhost:8080](http://localhost:8080). Press `Ctrl+C` in the terminal to stop the server. No web framework or JavaScript is required.

## Run the slot allocation test

```powershell
javac -cp "lib\mysql-connector-j-8.4.0.jar" -d out src\*.java
javac -cp "out;lib\mysql-connector-j-8.4.0.jar" -d out tests\ParkingLotAllocationTest.java
java -cp "out;lib\mysql-connector-j-8.4.0.jar" ParkingLotAllocationTest
```

The test checks first-fit allocation, truck consecutive slots, skipping occupied spaces, and a full lot.

The original menu-driven console app can still be started with:

```powershell
java -cp "out;lib\mysql-connector-j-8.4.0.jar" Main
```

## Features

- Browser dashboard for parking and exits
- Park bike, car, and truck vehicles
- Allocate first available appropriate slot
- Enforce truck two-slot requirement
- Generate unique ticket numbers
- Calculate charge using time difference and 8-hour cap
- Handle parking-full exception
- Manage MySQL persistence through JDBC
- Display slot map, summary, history, and payment history

## ER Diagram

```text
vehicles (1) ───< parking_tickets (many)
   │                     │
   │                     └──< payments (1)
   │
   └──< parking_slots (many) >──< ticket_slots (many)
```

- `vehicles` stores registration and owner details.
- `parking_slots` stores slot state and active vehicle mapping.
- `parking_tickets` stores ticket and billing data.
- `ticket_slots` links ticket to reserved slots.
- `payments` records collected charges.

## Browser Interface

The dashboard displays live slot availability, revenue and ticket/payment history. Use the **Park a vehicle** form to issue a ticket and the **Vehicle exit** form to complete it and record payment.

## Notes

This project intentionally keeps the design simple and beginner-friendly. The application uses arrays for slot allocation and MySQL for permanent storage.
