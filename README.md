# ParkEase – Smart Parking Lot Management System

ParkEase is a simple Java console-based parking application that follows the project requirement for managing 20 parking slots, vehicle types, ticket generation, billing, and MySQL-backed persistence.

## Project Structure

- `src/` – Java source files
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

Edit `config/db.properties` and update the username/password if needed:

```properties
db.url=jdbc:mysql://localhost:3306/parkease_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=
```

If you use a password-protected MySQL user, replace `db.user` and `db.password` accordingly.

## Compile

From the project root:

```bash
javac -cp "lib/mysql-connector-j-8.4.0.jar" -d out src/*.java
```

## Run

```bash
java -cp "out;lib/mysql-connector-j-8.4.0.jar" Main
```

## Features

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

## Sample Console Output

```text
========== PARKEASE ==========
1. Park Vehicle
2. Exit Vehicle
3. View Parking Slot Map
4. View Summary
5. View Parking History
6. View Payment History
7. Quit
Enter your choice: 1
Vehicle type (1-Bike, 2-Car, 3-Truck): 2
Vehicle registration number: TN37AB1234
Owner name: Aman
Ticket generated successfully.
Ticket Number: T-1001
Allocated Slot(s): [1]
```

## Notes

This project intentionally keeps the design simple and beginner-friendly. The application uses arrays for slot allocation and MySQL for permanent storage.
