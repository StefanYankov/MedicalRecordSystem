# Medical Record System

## Overview

This project is the **final project** for the university course:  
**[CSCB869 Java Web Services - Autumn Semester 2022/2023](https://ecatalog.nbu.bg/default.asp?V_Year=2022&YSem=7&Spec_ID=1593&Mod_ID=&PageShow=coursepresent&P_Menu=courses_part2&Fac_ID=3&M_PHD=&P_ID=832&TabIndex=&K_ID=13033&K_TypeID=10&l=1)**.

## Project Introduction

The **Medical Record System** is a Java-based web application developed as a final project for the **CSCB869 Java Web Services** course at the **New Bulgarian University**. It provides a centralized platform for managing patient medical records, including illness history, doctor visits, diagnoses, treatments, and sick leaves. The system supports various user roles (Patient, Doctor, and Admin) with distinct access levels and features, secured by Keycloak and OAuth2.

## Table of Contents

- [Project Goal](#project-goal)
- [Live Demo](#live-demo)
- [API Documentation (Swagger UI)](#api-documentation-swagger-ui)
- [Features](#features)
- [Architecture & Technologies](#architecture--technologies)
- [User Roles](#user-roles)
- [MVC User Stories](#mvc-user-stories)
- [API User Stories](#api-user-stories)
- [Installation & Setup](#installation--setup)
- [Keycloak Setup Details](#keycloak-setup-details)
- [Usage](#usage)
- [Utility Scripts](#utility-scripts)
- [Deployment to Azure](#deployment-to-azure)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgments](#acknowledgments)
- [Repository](#repository)

## Project Goal

The project's main goal is to create a comprehensive system for managing patient medical records. This includes tracking the history of illnesses for each patient, the doctors who have examined them, the treatments applied, and any sick leave issued. The system provides distinct functionalities for patients, doctors, and administrators, ensuring data is both accessible and secure based on the user's role.

## Live Demo

The Medical Record System application is deployed and accessible live on Microsoft Azure.

**Application URL:** [Placeholder - Will be updated with your Azure App Service URL]
**Swagger UI URL:** [Placeholder - Will be updated with your Azure App Service URL]/swagger-ui.html

**Note:** For authentication on the live demo, Keycloak is running alongside the application in Azure. You will be redirected to the Keycloak login page when accessing secured parts of the application or attempting to authorize in Swagger UI.


## Features

-   **Role-Based Access Control**: Secure access for Patients, Doctors, and Admins using Keycloak and Spring Security.
-   **Patient Profile Management**: Patients can complete their profile by providing their EGN and selecting a General Practitioner.
-   **Doctor Profile Management**: Doctors can complete and edit their professional profile, including specialties, GP status, and a profile image.
-   **Admin Dashboard**: A central hub for administrators to view system statistics and manage all data.
-   **Visit Scheduling & Documentation**: Patients can schedule visits with doctors, and doctors can document visit details, including diagnoses, treatments, and sick leave.
-   **Medical History**: Both patients and doctors can view a patient's complete medical history.
-   **Comprehensive Reporting**: Admins can generate various reports on diagnoses, doctor activity, patient distribution, and sick leave trends.
-   **Full CRUD Operations**: Admins have full control over all core entities (Doctors, Patients, Diagnoses, Visits, etc.).
-   **RESTful API**: A comprehensive API for programmatic access to all system functionalities, documented with OpenAPI.

## Architecture & Technologies

The application is built using a layered architecture to ensure a clean separation of concerns.

-   **Backend**: **Spring Boot 3**
-   **Security**: **Spring Security 6**, **Keycloak**, **OAuth2 & OIDC**
-   **Database**: **JPA (Hibernate)** with **MySQL**
-   **Frontend**: **Thymeleaf**, **Bootstrap**, **AdminLTE** (for admin panel)
-   **Build Tool**: **Gradle**
-   **API Documentation**: **OpenAPI 3.0 (springdoc)**
-   **Image Storage**: **Cloudinary**
-   **Development Email Server**: **Mailhog**
-   **Testing**: **JUnit 5**, **Mockito**, **Testcontainers**

## User Roles

-   **Patient**: Can view their own medical history, schedule, and cancel appointments.
-   **Doctor**: Can view any patient's history, document visits they have conducted, and manage their own profile.
-   **Admin**: Has full access to view and manage all data in the system and view aggregated reports.

## MVC User Stories

### Persona: Unauthenticated User / Visitor

*   **As an unauthenticated user, I want to view the homepage (`/`)** so that I can see general information and available medical specialties.
*   **As an unauthenticated user, I want to search for doctors by specialty (`/doctors/search`)** so that I can find doctors relevant to my needs.
*   **As an unauthenticated user, I want to register for a new account** so that I can become a patient or doctor in the system.
*   **As an unauthenticated user, I want to log in** so that I can access my personalized features.

### Persona: Patient

*   **As a patient, upon first login, I want to complete my profile (`/profile/complete`)** by providing my EGN and selecting my General Practitioner.
*   **As a patient, I want to view my personal dashboard (`/profile/dashboard`)** so that I can see my upcoming visits.
*   **As a patient, I want to view my medical history (`/profile/history`)** so that I can see all my past visits, diagnoses, and treatments.
*   **As a patient, I want to schedule a visit with a specific doctor (`/visits/schedule/{doctorId}`)** so that I can book an appointment.
*   **As a patient, I want to cancel my scheduled visit (`/visits/{id}/cancel`)** so that I can manage my appointments.
*   **As a patient, I want to view the details of a specific visit (`/visits/{id}`)** so that I can review its information.

### Persona: Doctor

*   **As a doctor, upon first login, I want to complete my professional profile (`/doctor/profile/complete`)** by providing my Unique ID Number and specialties.
*   **As a doctor, I want to view my personal dashboard (`/doctor/dashboard`)** so that I can see my scheduled visits.
*   **As a doctor, I want to view a list of all patients (`/doctor/patients`)** so that I can access their records.
*   **As a doctor, I want to view a patient's medical history (`/doctor/patients/{id}/history`)** so that I can review their past health information.
*   **As a doctor, I want to document a specific visit (`/doctor/visits/{visitId}/document`)** by adding diagnoses, treatments, and sick leave details.
*   **As a doctor, I want to edit my own profile (`/doctor/profile/edit`)** to update my personal and professional details, including image and specialties.

### Persona: Admin

*   **As an admin, I want to view the admin dashboard (`/admin/dashboard`)** so that I can see key system metrics and unapproved doctors.
*   **As an admin, I want to manage all core data**, including performing **Create, Read, Update, and Delete (CRUD)** operations on Doctors, Patients, Diagnoses, Visits, Sick Leaves, and Specialties.
*   **As an admin, I want to approve newly registered doctors** to grant them full system access.
*   **As an admin, I want to access a comprehensive reporting section (`/admin/reports`)** to gain insights into patient diagnoses, doctor activity, and sick leave trends.

---

## API Documentation (Swagger UI)

The application's RESTful API is documented using OpenAPI 3.0. An interactive Swagger UI is available for exploring and testing the endpoints.

-   **Swagger UI URL (Local):** `http://localhost:8080/swagger-ui.html`

To test secured endpoints, use the "Authorize" button and provide a valid JWT token from Keycloak for the desired user role.

---

## Installation & Setup

1.  **Prerequisites**:
    *   Java 21+
    *   Docker Desktop
    *   Gradle
    *   **Azure Account (for deployment, if applicable)**

2.  **Clone the repository**:
    ```bash
    git clone https://github.com/StefanYankov/MedicalRecordSystem
    ```

3.  **Environment Variables (`.env` file)**:
    Create a `.env` file in the project root with the following variables. These are crucial for local development and are also configured as Application Settings in Azure App Service for deployment.
    ```
    KEYCLOAK_CLIENT_SECRET=your_keycloak_client_secret
    SENDGRID_API_KEY=your_sendgrid_api_key
    CLOUDINARY_CLOUD_NAME=your_cloudinary_cloud_name
    CLOUDINARY_API_KEY=your_cloudinary_api_key
    CLOUDINARY_API_SECRET=your_cloudinary_api_secret
    ```
    *(Note: For local development, you can find a dummy `KEYCLOAK_CLIENT_SECRET` in the `keycloak/medical-system-realm.json` file under the `clients` section for `medical-record-system`.)*

4.  **Run Infrastructure (Docker Compose)**:
    The required services (Keycloak, MySQL, Mailhog) are defined in the `docker-compose.yml` file.
    ```bash
    docker-compose up -d
    ```

5.  **Configure Keycloak Realm**:
    *   Navigate to the Keycloak admin console at `http://localhost:8081`.
    *   Import the realm configuration file located at `/keycloak/medical-system-realm.json`. This will set up the `medical-system` realm, clients, and roles.
    *   **Important for Swagger UI (Local):** For the `medical-record-system` client, ensure `http://localhost:8080/swagger-ui/oauth2-redirect.html` is added to the "Valid Redirect URIs" list (or use `http://localhost:8080/*` for local development).

6.  **Build and Run the Application**:
    The application uses `application.yml` as its base configuration, with `application-dev.yml` overriding development-specific settings.
    ```bash
    ./gradlew bootRun
    ```
    The application will be available at `http://localhost:8080`.

**Note on Local Development:** For information on resetting your local database or handling potential startup timing issues with Keycloak, please see the [Utility Scripts](#utility-scripts) section.

## Keycloak Setup Details

The project uses Keycloak for authentication and authorization. The `keycloak/medical-system-realm.json` file contains the full configuration for the `medical-system` realm, including:

*   **Realm Name:** `medical-system`
*   **Client ID:** `medical-record-system`
*   **Roles:** `ADMIN`, `DOCTOR`, `PATIENT` (assigned as client roles to the `medical-record-system` client).
*   **Default User:** The realm does not come with default users. You will need to create users (e.g., `admin`, `doctor_user`, `patient_user`) in the Keycloak admin console and assign them the respective client roles.

**Admin Console:** `http://localhost:8081` (default credentials: `admin`/`admin`)

## Usage

-   **Homepage**: `http://localhost:8080`
-   **Admin Panel**: `http://localhost:8080/admin`
-   **Doctor Dashboard**: `http://localhost:8080/doctor/dashboard`
-   **Patient Dashboard**: `http://localhost:8080/profile/dashboard`
-   **Mailhog UI**: `http://localhost:8025` (to view captured emails)

## Utility Scripts

This project includes utility scripts to help with common development tasks.

### `database_truncate.sql`

This SQL script is used to completely wipe all data from the application's tables while keeping the table structures intact. It is useful for resetting your local database to a clean state without having to drop and recreate the entire database.

**Usage:**

You can execute this script using a MySQL client of your choice (e.g., MySQL Workbench, DBeaver, or the command line) connected to your local `medical_db` database.

### `wait-for-it.sh`

This is a simple shell script that waits for a specific host and port to become available before executing a command.

**Purpose in this Project:**

In the `docker-compose.yml` file, the main application (`medical-record-system`) depends on Keycloak. However, Keycloak can sometimes take a significant amount of time to start up. The `depends_on` condition `service_started` only waits for the container to start, not for the Keycloak application *inside* the container to be fully ready to accept connections.

If the Spring Boot application starts faster than Keycloak, it will fail at startup when it tries to connect to Keycloak for its OIDC configuration. The `wait-for-it.sh` script solves this "race condition".

**How it would be used (Example `docker-compose.yml` modification):**

To use the script, you would modify the `command` for the `medical-record-system` service in your `docker-compose.yml` like this:

```yaml
# In docker-compose.yml
services:
  # ... other services
  medical-record-system:
    # ... other properties
    command: ["./wait-for-it.sh", "keycloak:8080", "--", "java", "-jar", "app.jar"]
```

This command tells the application container: "Before you run the `java -jar app.jar` command, first run the `wait-for-it.sh` script and wait until the service named `keycloak` is reachable on port `8080`."

## Deployment to Azure

The application is deployed to Microsoft Azure using Azure App Service, a Platform-as-a-Service (PaaS) offering. This provides a managed environment for running the Spring Boot application without the need for Docker containers or virtual machine management.

**Key Azure Services Used:**
*   **Azure App Service:** Hosts the Spring Boot application.
*   **Azure Database for MySQL (Flexible Server):** Provides the managed MySQL database.
*   **Keycloak (Deployed to Azure):** For authentication and authorization in the live environment.

**Deployment Process:**
The deployment is automated using **GitHub Actions**. Pushing code to the `main` branch triggers a CI/CD pipeline that builds the application and deploys it directly to the Azure App Service.

---

## Contributing

As this is a university course project contributing is generally not required.

---

## License

The project is licensed under MIT License. See the **[LICENSE](https://github.com/StefanYankov/MedicalRecordSystem/blob/main/LICENSE)** file for details.

---

## Acknowledgments

- This project was developed as part of the **CSCB869 Java Web Services** course at [New Bulgarian University](https://nbu.bg/).
- Special thanks to the course instructor for creating the project requirements.

---

## Repository

GitHub Repository: [https://github.com/StefanYankov/MedicalRecordSystem](https://github.com/StefanYankov/MedicalRecordSystem)

---
