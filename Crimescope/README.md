# CrimeScope — Crime Hotspot Analysis and Risk Forecasting System

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.4-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-blue.svg)](https://maven.apache.org/)
[![Thymeleaf](https://img.shields.io/badge/UI-Thymeleaf-green.svg)](https://www.thymeleaf.org/)
[![Plotly](https://img.shields.io/badge/Plotly-2.35-3F4F75.svg)](https://plotly.com/)
[![SQLite](https://img.shields.io/badge/SQLite-Local_Storage-003B57.svg)](https://www.sqlite.org/)

**CrimeScope** is an end-to-end data science application and spatial intelligence command center designed for police patrol optimization, hotspot identification, and predictive crime risk forecasting. Centered around the Coimbatore metropolitan district in Tamil Nadu, India, CrimeScope features density-based spatial clustering (DBSCAN), supervised machine learning (Random Forest), and interactive dark-themed visual analytics built on a modern **Spring Boot 3** and **Java 21** architecture.

---

## 🎯 Key Capabilities & Architecture

1. **Automatic Synthetic Data Generation & SQLite Storage**
   - Automatically initializes and manages **6,000 realistic crime incident records** with a fixed random seed (`42`) for reproducibility.
   - Saves records into `data/crime_data.csv` and persists them into a local SQLite database `data/crimescope.db`.
   - Simulates spatio-temporal patterns across 14 Coimbatore neighborhoods (Gandhipuram, RS Puram, Peelamedu, Ukkadam, Town Hall, etc.) across 7 crime categories (Theft, Burglary, Robbery, Assault, Vehicle Theft, Fraud, Vandalism).

2. **Geospatial Hotspot Clustering (DBSCAN)**
   - Utilizes pure-Java **DBSCAN** density clustering with spherical Haversine metric calculations ($\varepsilon$ in kilometers) over geographic coordinates.
   - Discovers dense crime hotspots and isolates noise/outlier incidents.
   - Computes cluster centroids, dominant crime types, incident percentages, and area vulnerability rankings.

3. **Supervised Crime Risk Forecasting Model**
   - Implements a pure-Java ensemble **Random Forest Classifier** (60 decision trees) trained to forecast operational risk tiers (`Low`, `Medium`, `High`).
   - Prevents target leakage by splitting data into independent 80% train and 20% test partitions before fitting encoders or estimating baselines.
   - Evaluates out-of-sample metrics: Accuracy (73.33%), Macro/Weighted Precision, Recall, F1-Score, Confusion Matrix, and Gini Feature Importance.
   - Offers real-time simulation assessing risk probability distributions for any chosen area, time slot, and physical environment.

4. **Tactical Command Center Dashboard**
   - Dark intelligence-dashboard aesthetic with responsive layout.
   - Interactive Plotly Mapbox cartographic maps, timeline trend charts, 24-hour frequency distributions, and Day × Hour spatio-temporal heatmap matrices.
   - Five operational modules:
     1. **Overview Dashboard**
     2. **Crime Hotspot Map**
     3. **Crime Analytics**
     4. **Risk Forecasting**
     5. **Dataset Explorer** (with CSV download)

---

## 📂 Project Structure

```
Crimescope/
├── pom.xml                                              # Maven build configuration (Spring Boot 3.3.4, Java 21)
├── README.md                                            # Project documentation and execution guide
├── CrimeScope_Project_Report_Nitesh_S_722825243136.docx # Complete academic project report (Word DOCX)
├── CrimeScope_Project_Report_Nitesh_S_722825243136.pdf  # Complete academic project report (PDF)
│
├── data/
│   ├── crime_data.csv                                   # Crime incident dataset (6,000 records)
│   └── crimescope.db                                    # Persistent SQLite database storage
│
└── src/main/
    ├── java/com/crimescope/
    │   ├── CrimeScopeApplication.java                   # Main Spring Boot application runner
    │   ├── config/
    │   │   └── WebConfig.java                           # Web MVC resource handler configuration
    │   ├── controller/
    │   │   ├── DashboardController.java                 # Web MVC Controller: Renders main command center (GET /)
    │   │   └── CrimeApiController.java                  # REST Controller: /api/filter, /api/hotspots, /api/predict, etc.
    │   ├── service/
    │   │   ├── CrimeDataService.java                    # Ingestion, SQLite caching, multi-parameter filtering
    │   │   ├── PreprocessingService.java                # Temporal feature engineering, sin/cos cyclical encoding
    │   │   ├── HotspotService.java                      # Haversine DBSCAN spatial density clustering
    │   │   ├── RiskForecastingService.java              # Ensemble Random Forest classifier (60 trees, Gini splitting)
    │   │   └── AnalyticsService.java                    # Plotly chart generation (JSON/HTML)
    │   ├── repository/
    │   │   └── CrimeRecordRepository.java               # Spring JdbcTemplate repository for SQLite 'crimes' table
    │   └── model/
    │       ├── CrimeRecord.java                         # Incident entity model
    │       ├── HotspotCluster.java                      # DBSCAN hotspot cluster summary DTO
    │       ├── AreaRanking.java                         # Area vulnerability ranking DTO
    │       ├── DbscanStats.java                         # Clustering metrics DTO
    │       ├── FilterRequest.java & FilterResponse.java # Dynamic filter DTOs
    │       ├── HotspotRequest.java & HotspotResponse.java # Spatial reclustering DTOs
    │       ├── PredictionRequest.java & PredictionResponse.java # Risk simulation DTOs
    │       ├── FeatureImportance.java                   # Predictor variable weighting DTO
    │       └── ModelMetrics.java                        # Model evaluation DTO
    │
    └── resources/
        ├── application.properties                       # SQLite datasource, Hikari pool, port 8501 config
        ├── templates/
        │   └── index.html                               # Modern Thymeleaf dashboard template
        └── static/
            ├── css/style.css                            # Dark command-center stylesheet
            └── js/main.js                               # Interactive navigation, filters & Plotly handlers
```

---

## 🚀 Build & Run Guide

### Prerequisites
- **JDK 21** installed and configured (`javac -version`)
- **Apache Maven 3.9+** installed (`mvn -version`)

### 1. Build & Package
```powershell
cd "C:\Users\Nitesh\OneDrive\Desktop\Projects\Crimescope"
mvn clean package -DskipTests
```

### 2. Run Application
```powershell
java -jar target\crimescope-1.0.0.jar
```

The application automatically starts Tomcat on **port 8501**:
👉 **Open [http://localhost:8501](http://localhost:8501)** in your web browser.

---

## 🔬 Scientific Methodology & Academic Disclaimer

- **Reproducibility:** Seed `42` ensures identical dataset generation across runs.
- **Target Leakage Prevention:** Target labels are formed using historical spatio-temporal terciles; encoders and baselines are fit exclusively on the training split.
- **Synthetic Data Notice:** All records in this project are synthetically generated for academic demonstration and machine learning benchmarking. They do **not** represent confidential police department records or real criminal investigations.
