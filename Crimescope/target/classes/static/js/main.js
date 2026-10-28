/**
 * CrimeScope — Interactive Client Application
 * Handles module navigation, multi-level temporal filtering (Year, Month, Date),
 * interactive Plotly graph click events, risk forecasting, and dataset operations.
 */

document.addEventListener("DOMContentLoaded", () => {
    initNavigation();
    initTabs();
    initFilterHandlers();
    initForecastingForm();
    initHotspotControls();
    initDatasetSearch();
    setTimeout(attachGraphClickListeners, 600);
});

// Module Navigation
function initNavigation() {
    const navLinks = document.querySelectorAll(".nav-link");
    const moduleViews = document.querySelectorAll(".module-view");

    navLinks.forEach(link => {
        link.addEventListener("click", (e) => {
            e.preventDefault();
            const targetModuleId = link.getAttribute("data-target");

            navLinks.forEach(l => l.classList.remove("active"));
            moduleViews.forEach(v => v.classList.remove("active"));

            link.classList.add("active");
            const targetView = document.getElementById(targetModuleId);
            if (targetView) {
                targetView.classList.add("active");
                window.dispatchEvent(new Event('resize'));
                setTimeout(attachGraphClickListeners, 400);
            }
        });
    });
}

// Tab navigation within Crime Analytics module
function initTabs() {
    const tabBtns = document.querySelectorAll(".tab-btn");
    tabBtns.forEach(btn => {
        btn.addEventListener("click", () => {
            const targetTab = btn.getAttribute("data-tab");
            const parentModule = btn.closest(".module-view");

            parentModule.querySelectorAll(".tab-btn").forEach(b => b.classList.remove("active"));
            parentModule.querySelectorAll(".tab-content").forEach(c => c.classList.remove("active"));

            btn.classList.add("active");
            const content = parentModule.querySelector(`#${targetTab}`);
            if (content) {
                content.classList.add("active");
                window.dispatchEvent(new Event('resize'));
            }
        });
    });
}

// Global Filter State Machine
const FILTER_STATE = {
    mode: "all", // "all" | "single_day" | "range" | "year_month"
    exact_date: "",
    start_date: "2023-01-01",
    end_date: "2024-12-30",
    year: "ALL",
    month: "ALL",
    area: "ALL",
    crime_type: "ALL"
};

// Robust Plotly Container Updater: Replaces HTML and executes Plotly scripts synchronously
function updatePlotlyContainer(containerId, htmlString) {
    const container = document.getElementById(containerId);
    if (!container) return;
    if (!htmlString || !htmlString.trim()) {
        container.innerHTML = '<div style="color:#94a3b8; text-align:center; padding:45px; font-size:13px;">No data matching the active filter criteria.</div>';
        return;
    }

    // Purge previous Plotly instance to prevent memory leaks and orphaned click handlers
    const existingPlot = container.querySelector(".plotly-graph-div");
    if (existingPlot && window.Plotly && typeof window.Plotly.purge === "function") {
        try { window.Plotly.purge(existingPlot); } catch (e) {}
    }

    // Insert new HTML
    container.innerHTML = htmlString;

    // Execute scripts synchronously in global window scope
    const scripts = container.querySelectorAll("script");
    scripts.forEach(s => {
        const code = s.innerText || s.textContent;
        if (code) {
            try {
                window.eval(code);
            } catch (err) {
                console.error("Plotly render error in #" + containerId + ":", err);
            }
        }
    });
}

// Central Filter Application Function
async function applyFilter(overrides = {}) {
    // 1. Update FILTER_STATE based on overrides
    if (overrides.exact_date !== undefined && overrides.exact_date !== "") {
        FILTER_STATE.mode = "single_day";
        FILTER_STATE.exact_date = overrides.exact_date;
        FILTER_STATE.year = "ALL";
        FILTER_STATE.month = "ALL";
        FILTER_STATE.start_date = "";
        FILTER_STATE.end_date = "";
    } else if (overrides.mode === "all") {
        FILTER_STATE.mode = "all";
        FILTER_STATE.exact_date = "";
        FILTER_STATE.year = "ALL";
        FILTER_STATE.month = "ALL";
        FILTER_STATE.start_date = "2023-01-01";
        FILTER_STATE.end_date = "2024-12-30";
    } else if ((overrides.year !== undefined && overrides.year !== "ALL") || (overrides.month !== undefined && overrides.month !== "ALL")) {
        FILTER_STATE.mode = "year_month";
        if (overrides.year !== undefined) FILTER_STATE.year = overrides.year;
        if (overrides.month !== undefined) FILTER_STATE.month = overrides.month;
        FILTER_STATE.exact_date = "";
        FILTER_STATE.start_date = "";
        FILTER_STATE.end_date = "";
    } else if ((overrides.start_date !== undefined && overrides.start_date !== "") || (overrides.end_date !== undefined && overrides.end_date !== "")) {
        if (overrides.start_date === "2023-01-01" && overrides.end_date === "2024-12-30") {
            FILTER_STATE.mode = "all";
        } else {
            FILTER_STATE.mode = "range";
        }
        if (overrides.start_date !== undefined) FILTER_STATE.start_date = overrides.start_date;
        if (overrides.end_date !== undefined) FILTER_STATE.end_date = overrides.end_date;
        FILTER_STATE.exact_date = "";
        FILTER_STATE.year = "ALL";
        FILTER_STATE.month = "ALL";
    }

    if (overrides.area !== undefined) {
        FILTER_STATE.area = overrides.area;
    }
    if (overrides.crime_type !== undefined) {
        FILTER_STATE.crime_type = overrides.crime_type;
    }

    // 2. Synchronize UI inputs to match FILTER_STATE
    syncFilterInputs({
        year: FILTER_STATE.year,
        month: FILTER_STATE.month,
        exactDate: FILTER_STATE.exact_date,
        startDate: FILTER_STATE.start_date,
        endDate: FILTER_STATE.end_date,
        area: FILTER_STATE.area,
        crimeType: FILTER_STATE.crime_type
    });

    // 3. Prepare payload for /api/filter
    const payload = {
        mode: FILTER_STATE.mode,
        exact_date: FILTER_STATE.mode === "single_day" ? FILTER_STATE.exact_date : "",
        year: FILTER_STATE.mode === "year_month" ? FILTER_STATE.year : "ALL",
        month: FILTER_STATE.mode === "year_month" ? FILTER_STATE.month : "ALL",
        start_date: (FILTER_STATE.mode === "range" || FILTER_STATE.mode === "all") ? FILTER_STATE.start_date : "",
        end_date: (FILTER_STATE.mode === "range" || FILTER_STATE.mode === "all") ? FILTER_STATE.end_date : "",
        area: FILTER_STATE.area,
        crime_type: FILTER_STATE.crime_type
    };

    const statusBadge = document.getElementById("active-filter-badge");
    if (statusBadge) {
        statusBadge.innerText = "Applying filter criteria...";
    }

    try {
        const resp = await fetch("/api/filter", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        const data = await resp.json();

        // Update KPI values
        if (document.getElementById("kpi-total-records")) {
            document.getElementById("kpi-total-records").innerText = Number(data.total_records).toLocaleString();
        }
        if (document.getElementById("kpi-hotspots")) {
            document.getElementById("kpi-hotspots").innerText = data.num_hotspots;
        }
        if (document.getElementById("kpi-highest-area")) {
            document.getElementById("kpi-highest-area").innerText = data.highest_risk_area;
        }
        if (document.getElementById("kpi-primary-crime")) {
            document.getElementById("kpi-primary-crime").innerText = data.most_common_crime;
        }

        // Update active filter badge text
        if (statusBadge && data.active_label) {
            statusBadge.innerText = data.active_label;
        }

        // Update Plotly charts synchronously with immediate script execution
        if (data.chart_category) {
            updatePlotlyContainer("chart-category-container", data.chart_category);
        }
        if (data.chart_timeline) {
            updatePlotlyContainer("chart-timeline-container", data.chart_timeline);
        }
        if (data.chart_hourly) {
            updatePlotlyContainer("chart-hourly-container", data.chart_hourly);
        }
        if (data.chart_areas) {
            updatePlotlyContainer("chart-areas-container", data.chart_areas);
        }
        if (data.chart_map && document.getElementById("map-container")) {
            updatePlotlyContainer("map-container", data.chart_map);
        }

        // Re-attach interactive click listeners
        setTimeout(attachGraphClickListeners, 250);

    } catch (err) {
        console.error("Filter update failed:", err);
        if (statusBadge) statusBadge.innerText = "Error loading filtered data";
    }
}

// Synchronize inputs across the overview panel
function syncFilterInputs({ year, month, exactDate, startDate, endDate, area, crimeType }) {
    const oYear = document.getElementById("overview-filter-year");
    if (oYear && year !== undefined) oYear.value = year;

    const oMonth = document.getElementById("overview-filter-month");
    if (oMonth && month !== undefined) oMonth.value = month;

    const oDate = document.getElementById("overview-filter-date");
    if (oDate && exactDate !== undefined) oDate.value = exactDate;

    const oStart = document.getElementById("overview-start-date");
    if (oStart && startDate !== undefined && startDate !== "") oStart.value = startDate;

    const oEnd = document.getElementById("overview-end-date");
    if (oEnd && endDate !== undefined && endDate !== "") oEnd.value = endDate;

    const oArea = document.getElementById("overview-filter-area");
    if (oArea && area !== undefined) oArea.value = area;

    const oCrime = document.getElementById("overview-filter-crime-type");
    if (oCrime && crimeType !== undefined) oCrime.value = crimeType;
}

// Global & Overview Filter Controls Initialization
function initFilterHandlers() {
    // 1. Day-by-Day Stepper Controls (Previous Day, Next Day, Latest Day)
    const prevDayBtn = document.getElementById("day-prev-btn");
    if (prevDayBtn) {
        prevDayBtn.addEventListener("click", () => {
            const currDateInput = document.getElementById("overview-filter-date");
            const curVal = currDateInput?.value || "2024-12-30";
            const curDt = new Date(curVal + "T00:00:00");
            curDt.setDate(curDt.getDate() - 1);
            const prevStr = curDt.toISOString().split("T")[0];
            if (prevStr >= "2023-01-01") {
                if (currDateInput) currDateInput.value = prevStr;
                document.querySelectorAll(".preset-btn").forEach(b => b.classList.remove("active"));
                applyFilter({ exact_date: prevStr });
            }
        });
    }

    const nextDayBtn = document.getElementById("day-next-btn");
    if (nextDayBtn) {
        nextDayBtn.addEventListener("click", () => {
            const currDateInput = document.getElementById("overview-filter-date");
            const curVal = currDateInput?.value || "2024-12-30";
            const curDt = new Date(curVal + "T00:00:00");
            curDt.setDate(curDt.getDate() + 1);
            const nextStr = curDt.toISOString().split("T")[0];
            if (nextStr <= "2024-12-30") {
                if (currDateInput) currDateInput.value = nextStr;
                document.querySelectorAll(".preset-btn").forEach(b => b.classList.remove("active"));
                applyFilter({ exact_date: nextStr });
            }
        });
    }

    const latestDayBtn = document.getElementById("day-latest-btn");
    if (latestDayBtn) {
        latestDayBtn.addEventListener("click", () => {
            const latestStr = "2024-12-30";
            const currDateInput = document.getElementById("overview-filter-date");
            if (currDateInput) currDateInput.value = latestStr;
            document.querySelectorAll(".preset-btn").forEach(b => b.classList.remove("active"));
            const sBtn = document.querySelector('.preset-btn[data-preset="single_day"]');
            if (sBtn) sBtn.classList.add("active");
            applyFilter({ exact_date: latestStr });
        });
    }

    // 2. Overview Apply Filters Button
    const rangeBtn = document.getElementById("overview-apply-range-btn");
    if (rangeBtn) {
        rangeBtn.addEventListener("click", () => {
            const area = document.getElementById("overview-filter-area")?.value || "ALL";
            const crimeType = document.getElementById("overview-filter-crime-type")?.value || "ALL";
            const sDate = document.getElementById("overview-start-date")?.value || "";
            const eDate = document.getElementById("overview-end-date")?.value || "";
            const year = document.getElementById("overview-filter-year")?.value || "ALL";
            const month = document.getElementById("overview-filter-month")?.value || "ALL";

            document.querySelectorAll(".preset-btn").forEach(b => b.classList.remove("active"));

            // Check what temporal mode the user explicitly interacted with
            if (year !== "ALL" || month !== "ALL") {
                applyFilter({ year: year, month: month, area: area, crime_type: crimeType });
            } else if (sDate && eDate && (sDate !== "2023-01-01" || eDate !== "2024-12-30")) {
                applyFilter({ start_date: sDate, end_date: eDate, area: area, crime_type: crimeType });
            } else {
                applyFilter({ mode: "all", area: area, crime_type: crimeType });
            }
        });
    }

    // 3. Quick Range Preset Buttons
    document.querySelectorAll(".preset-btn").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".preset-btn").forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            const preset = btn.getAttribute("data-preset");
            const maxDateStr = "2024-12-30";
            const minDateStr = "2023-01-01";
            const maxDateObj = new Date(maxDateStr + "T00:00:00");

            if (preset === "single_day") {
                const dateVal = document.getElementById("overview-filter-date")?.value || maxDateStr;
                applyFilter({ exact_date: dateVal });
                return;
            } else if (preset === "7d") {
                const s = new Date(maxDateObj);
                s.setDate(s.getDate() - 6);
                const startStr = s.toISOString().split("T")[0];
                applyFilter({ start_date: startStr, end_date: maxDateStr });
            } else if (preset === "14d") {
                const s = new Date(maxDateObj);
                s.setDate(s.getDate() - 13);
                const startStr = s.toISOString().split("T")[0];
                applyFilter({ start_date: startStr, end_date: maxDateStr });
            } else if (preset === "30d") {
                const s = new Date(maxDateObj);
                s.setDate(s.getDate() - 29);
                const startStr = s.toISOString().split("T")[0];
                applyFilter({ start_date: startStr, end_date: maxDateStr });
            } else if (preset === "latest_month") {
                applyFilter({ start_date: "2024-12-01", end_date: "2024-12-30" });
            } else if (preset === "all") {
                applyFilter({ mode: "all", start_date: minDateStr, end_date: maxDateStr });
            }
        });
    });

    // 4. Overview Panel "Reset to All Records" button
    const overviewResetBtn = document.getElementById("overview-reset-date-btn");
    if (overviewResetBtn) {
        overviewResetBtn.addEventListener("click", () => {
            document.querySelectorAll(".preset-btn").forEach(b => b.classList.remove("active"));
            const allBtn = document.querySelector('.preset-btn[data-preset="all"]');
            if (allBtn) allBtn.classList.add("active");
            if (document.getElementById("overview-filter-area")) document.getElementById("overview-filter-area").value = "ALL";
            if (document.getElementById("overview-filter-crime-type")) document.getElementById("overview-filter-crime-type").value = "ALL";
            if (document.getElementById("overview-filter-year")) document.getElementById("overview-filter-year").value = "ALL";
            if (document.getElementById("overview-filter-month")) document.getElementById("overview-filter-month").value = "ALL";
            if (document.getElementById("overview-start-date")) document.getElementById("overview-start-date").value = "2023-01-01";
            if (document.getElementById("overview-end-date")) document.getElementById("overview-end-date").value = "2024-12-30";
            applyFilter({
                mode: "all",
                start_date: "2023-01-01",
                end_date: "2024-12-30",
                exact_date: "",
                year: "ALL",
                month: "ALL",
                area: "ALL",
                crime_type: "ALL"
            });
        });
    }

    // 5. Quick Auto-Filter on Date Input change (Single Day Visibility)
    const overviewDateInput = document.getElementById("overview-filter-date");
    if (overviewDateInput) {
        overviewDateInput.addEventListener("change", (e) => {
            if (e.target.value) {
                document.querySelectorAll(".preset-btn").forEach(b => b.classList.remove("active"));
                applyFilter({ exact_date: e.target.value });
            }
        });
    }

    // 6. Quick Auto-Filter on Area & Crime Category Dropdowns
    const overviewAreaSelect = document.getElementById("overview-filter-area");
    if (overviewAreaSelect) {
        overviewAreaSelect.addEventListener("change", () => {
            applyFilter({ area: overviewAreaSelect.value });
        });
    }

    const overviewCrimeSelect = document.getElementById("overview-filter-crime-type");
    if (overviewCrimeSelect) {
        overviewCrimeSelect.addEventListener("change", () => {
            applyFilter({ crime_type: overviewCrimeSelect.value });
        });
    }

    // 7. Quick Auto-Filter on Year & Month Dropdowns
    const overviewYearSelect = document.getElementById("overview-filter-year");
    const overviewMonthSelect = document.getElementById("overview-filter-month");

    if (overviewYearSelect) {
        overviewYearSelect.addEventListener("change", () => {
            document.querySelectorAll(".preset-btn").forEach(b => b.classList.remove("active"));
            applyFilter({
                year: overviewYearSelect.value,
                month: overviewMonthSelect?.value || "ALL"
            });
        });
    }

    if (overviewMonthSelect) {
        overviewMonthSelect.addEventListener("change", () => {
            document.querySelectorAll(".preset-btn").forEach(b => b.classList.remove("active"));
            applyFilter({
                year: overviewYearSelect?.value || "ALL",
                month: overviewMonthSelect.value
            });
        });
    }
}

// Interactive Plotly Graph Click Listeners
function attachGraphClickListeners() {
    // 1. Timeline Chart Click Handler (Isolate specific single day across all charts)
    const timelineContainer = document.getElementById("chart-timeline-container");
    if (timelineContainer) {
        const plotDiv = timelineContainer.querySelector(".plotly-graph-div");
        if (plotDiv && !plotDiv.dataset.clickBound) {
            plotDiv.dataset.clickBound = "true";
            plotDiv.on("plotly_click", (data) => {
                if (data && data.points && data.points.length > 0) {
                    const pt = data.points[0];

                    // Check if clicked point is from Month-to-Month trendline
                    let isMonthPoint = false;
                    let clickedYear = "";
                    let clickedMonth = "";

                    if (Array.isArray(pt.customdata) && pt.customdata.length >= 5) {
                        isMonthPoint = true;
                        clickedYear = String(pt.customdata[3]);
                        clickedMonth = String(pt.customdata[4]);
                    } else if (pt.customdata && /^\d{4}-\d{2}$/.test(String(pt.customdata))) {
                        isMonthPoint = true;
                        const ym = String(pt.customdata);
                        clickedYear = ym.substring(0, 4);
                        clickedMonth = String(parseInt(ym.substring(5, 7), 10));
                    }

                    if (isMonthPoint && clickedYear && clickedMonth) {
                        console.log("Drilling into month day-by-day from monthly trendline:", clickedYear, clickedMonth);
                        document.querySelectorAll(".preset-btn").forEach(b => b.classList.remove("active"));
                        applyFilter({ year: clickedYear, month: clickedMonth, exact_date: "", start_date: "", end_date: "" });
                        return;
                    }

                    // Otherwise check for single calendar day click
                    let dateStr = "";
                    if (pt.customdata && /^\d{4}-\d{2}-\d{2}/.test(String(pt.customdata))) {
                        dateStr = String(pt.customdata).substring(0, 10);
                    } else if (pt.x && /^\d{4}-\d{2}-\d{2}/.test(String(pt.x))) {
                        dateStr = String(pt.x).substring(0, 10);
                    }

                    if (dateStr) {
                        console.log("Drilling into single day from graph click:", dateStr);
                        document.querySelectorAll(".preset-btn").forEach(b => b.classList.remove("active"));
                        applyFilter({ exact_date: dateStr, start_date: "", end_date: "", year: "ALL", month: "ALL" });
                    }
                }
            });
        }
    }

    // 2. Category Donut Chart Click Handler
    const categoryContainer = document.getElementById("chart-category-container");
    if (categoryContainer) {
        const catPlotDiv = categoryContainer.querySelector(".plotly-graph-div");
        if (catPlotDiv && !catPlotDiv.dataset.clickBound) {
            catPlotDiv.dataset.clickBound = "true";
            catPlotDiv.on("plotly_click", (data) => {
                if (data && data.points && data.points.length > 0) {
                    const label = data.points[0].label;
                    if (label) {
                        console.log("Category clicked:", label);
                        applyFilter({ crime_type: label });
                    }
                }
            });
        }
    }

    // 3. Top Areas Bar Chart Click Handler
    const areasContainer = document.getElementById("chart-areas-container");
    if (areasContainer) {
        const areaPlotDiv = areasContainer.querySelector(".plotly-graph-div");
        if (areaPlotDiv && !areaPlotDiv.dataset.clickBound) {
            areaPlotDiv.dataset.clickBound = "true";
            areaPlotDiv.on("plotly_click", (data) => {
                if (data && data.points && data.points.length > 0) {
                    const areaName = data.points[0].y;
                    if (areaName) {
                        console.log("Area clicked:", areaName);
                        applyFilter({ area: areaName });
                    }
                }
            });
        }
    }
}

// Hotspot Clustering Controls
function initHotspotControls() {
    const updateBtn = document.getElementById("update-hotspots-btn");
    if (!updateBtn) return;

    updateBtn.addEventListener("click", async () => {
        updateBtn.innerText = "Reclustering...";
        updateBtn.disabled = true;

        const epsKm = parseFloat(document.getElementById("hotspot-eps-slider").value);
        const minSamples = parseInt(document.getElementById("hotspot-min-pts-slider").value);
        const colorBy = document.getElementById("hotspot-color-mode").value;

        try {
            const resp = await fetch("/api/hotspots", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ eps_km: epsKm, min_samples: minSamples, color_by: colorBy })
            });
            const data = await resp.json();

            // Update summary metrics
            document.getElementById("hotspot-stat-clusters").innerText = data.stats.num_clusters;
            document.getElementById("hotspot-stat-crimes").innerText = Number(data.stats.hotspot_crimes).toLocaleString();
            document.getElementById("hotspot-stat-noise").innerText = Number(data.stats.noise_crimes).toLocaleString();
            document.getElementById("hotspot-stat-noise-pct").innerText = data.stats.noise_pct + "%";

            // Update Map
            if (data.map_html) {
                updatePlotlyContainer("map-container", data.map_html);
            }

            // Update Hotspot Table
            if (data.table_html) {
                document.getElementById("hotspot-table-container").innerHTML = data.table_html;
            }

            setTimeout(attachGraphClickListeners, 250);
        } catch (err) {
            console.error("Hotspot clustering update error:", err);
        } finally {
            updateBtn.innerText = "Re-run Spatial Clustering";
            updateBtn.disabled = false;
        }
    });
}

// Risk Forecasting Module
function initForecastingForm() {
    const forecastBtn = document.getElementById("run-forecast-btn");
    if (!forecastBtn) return;

    forecastBtn.addEventListener("click", async () => {
        forecastBtn.innerText = "Computing Forecast...";
        forecastBtn.disabled = true;

        const area = document.getElementById("pred-area").value;
        const crimeType = document.getElementById("pred-crime-type").value;
        const locationType = document.getElementById("pred-location-type").value;
        const dayOfWeek = document.getElementById("pred-day").value;
        const hour = parseInt(document.getElementById("pred-hour").value);
        const month = parseInt(document.getElementById("pred-month").value);

        try {
            const resp = await fetch("/api/predict", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    area: area,
                    crime_type: crimeType,
                    location_type: locationType,
                    day_of_week: dayOfWeek,
                    hour: hour,
                    month: month
                })
            });
            const result = await resp.json();

            // Render prediction output
            const outContainer = document.getElementById("forecast-results-container");
            outContainer.style.display = "block";

            const tierBadge = document.getElementById("forecast-tier-badge");
            tierBadge.innerText = `${result.risk_label.toUpperCase()} RISK`;
            tierBadge.className = `badge-${result.risk_label.toLowerCase()}`;

            document.getElementById("forecast-confidence").innerText = `Confidence: ${result.confidence}%`;
            document.getElementById("forecast-baseline").innerText = `${result.historical_rating} Baseline`;
            document.getElementById("forecast-baseline-sub").innerText = `Historical Score: ${result.historical_baseline_score} / 2.0`;
            
            const timeStr = `${String(result.inputs.hour).padStart(2, '0')}:00 hrs`;
            document.getElementById("forecast-window").innerText = `${result.inputs.day_of_week}, ${timeStr}`;
            document.getElementById("forecast-window-sub").innerText = `Season: ${result.inputs.season} | Weekend: ${result.inputs.is_weekend ? 'Yes' : 'No'}`;

            // Progress bars
            const pLow = result.probabilities.Low * 100;
            const pMed = result.probabilities.Medium * 100;
            const pHigh = result.probabilities.High * 100;

            document.getElementById("prob-low-text").innerText = `${pLow.toFixed(1)}%`;
            document.getElementById("prob-low-bar").style.width = `${pLow}%`;

            document.getElementById("prob-med-text").innerText = `${pMed.toFixed(1)}%`;
            document.getElementById("prob-med-bar").style.width = `${pMed}%`;

            document.getElementById("prob-high-text").innerText = `${pHigh.toFixed(1)}%`;
            document.getElementById("prob-high-bar").style.width = `${pHigh}%`;

            // Tactical Directive Alert
            const directiveBox = document.getElementById("forecast-directive");
            directiveBox.className = `alert-box alert-${result.risk_label.toLowerCase()}`;
            if (result.risk_label === "High") {
                directiveBox.innerHTML = `🚨 <b>High Alert for ${area}:</b> Elevated probability (${pHigh.toFixed(1)}%) of serious incidents during ${dayOfWeek} at ${timeStr}. <b>Recommended:</b> Deploy proactive vehicular patrols and checkpoint surveillance near ${locationType}.`;
            } else if (result.risk_label === "Medium") {
                directiveBox.innerHTML = `⚠️ <b>Moderate Alert for ${area}:</b> Medium risk probability (${pMed.toFixed(1)}%). <b>Recommended:</b> Regular beat surveillance and visibility patrols.`;
            } else {
                directiveBox.innerHTML = `✅ <b>Normal Status for ${area}:</b> Low estimated risk probability (${pLow.toFixed(1)}%). Standard routine patrolling sufficient.`;
            }

            // Render feature importance
            if (result.feature_chart) {
                updatePlotlyContainer("forecast-feature-chart", result.feature_chart);
            }

        } catch (err) {
            console.error("Risk prediction error:", err);
        } finally {
            forecastBtn.innerText = "🚀 Run Risk Forecasting Analysis";
            forecastBtn.disabled = false;
        }
    });
}

// Dataset Search & Filter
function initDatasetSearch() {
    const searchInput = document.getElementById("dataset-search-input");
    const tableBody = document.getElementById("dataset-table-body");
    if (!searchInput || !tableBody) return;

    searchInput.addEventListener("input", () => {
        const query = searchInput.value.toLowerCase().trim();
        const rows = tableBody.querySelectorAll("tr");

        rows.forEach(row => {
            const text = row.innerText.toLowerCase();
            row.style.display = text.includes(query) ? "" : "none";
        });
    });
}

// Regenerate dataset confirmation
async function handleDatasetRegeneration() {
    const confirmBox = document.getElementById("confirm-regen-checkbox");
    if (!confirmBox || !confirmBox.checked) {
        alert("Please check the confirmation box before regenerating.");
        return;
    }

    if (!confirm("Are you sure you want to re-sample 6,000 synthetic records and reset the SQLite database?")) {
        return;
    }

    const btn = document.getElementById("regen-dataset-btn");
    btn.innerText = "Regenerating records & retraining model...";
    btn.disabled = true;

    try {
        const resp = await fetch("/api/regenerate", { method: "POST" });
        const res = await resp.json();
        if (res.status === "success") {
            alert("Dataset successfully regenerated and database updated! Reloading application...");
            window.location.reload();
        } else {
            alert("Error regenerating: " + res.message);
        }
    } catch (e) {
        alert("Network error regenerating dataset: " + e);
    } finally {
        btn.innerText = "Regenerate Dataset Now";
        btn.disabled = false;
    }
}

// Helper fallback to execute script tags
function executeEmbeddedScripts() {
    document.querySelectorAll(".plotly-graph-div script").forEach(s => {
        const code = s.innerText || s.textContent;
        if (code && !s.dataset.executed) {
            s.dataset.executed = "true";
            try { window.eval(code); } catch (e) {}
        }
    });
}
