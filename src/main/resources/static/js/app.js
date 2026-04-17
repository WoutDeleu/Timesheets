/* Timesheets — JS for interactive controls */

/**
 * Parse hour input: accepts "7:36" (H:MM) or "7.6" or "7,6" (decimal).
 * Returns a decimal number or NaN if invalid.
 */
function parseHours(input) {
    if (!input || !input.trim()) return NaN;
    var trimmed = input.trim();

    if (trimmed.indexOf(':') !== -1) {
        var parts = trimmed.split(':');
        var hours = parseInt(parts[0], 10);
        var minutes = parts.length > 1 ? parseInt(parts[1], 10) : 0;
        if (isNaN(hours) || isNaN(minutes)) return NaN;
        return hours + minutes / 60;
    }

    return parseFloat(trimmed.replace(',', '.'));
}

/**
 * Format decimal hours as H:MM string (e.g. 7.6 → "7:36", -7.6 → "-7:36").
 */
function formatHours(decimal) {
    if (isNaN(decimal) || decimal === null || decimal === undefined) return '';
    var totalMinutes = Math.round(decimal * 60);
    var sign = totalMinutes < 0 ? '-' : '';
    var absTotalMinutes = Math.abs(totalMinutes);
    var hours = Math.floor(absTotalMinutes / 60);
    var minutes = absTotalMinutes % 60;
    return sign + hours + ':' + (minutes < 10 ? '0' : '') + minutes;
}

/**
 * Attach blur handler to an hour input: normalize display to H:MM on blur.
 */
function attachHourBlur(input) {
    input.addEventListener('blur', function () {
        var val = parseHours(this.value);
        if (!isNaN(val) && val > 0) {
            this.value = formatHours(val);
        }
    });
}

/**
 * Before form submit: convert all hour inputs from H:MM to decimal for the server.
 */
function attachFormSubmitConversion(form) {
    form.addEventListener('submit', function () {
        form.querySelectorAll('input.hour-input').forEach(function (input) {
            var val = parseHours(input.value);
            if (!isNaN(val)) {
                input.value = val;
            }
        });
    });
}

/**
 * Daily form: update break default when project is selected.
 * If location is OFFICE, set break to project default; if HOME, set to 0.
 */
function updateBreakDefault(projectRadio) {
    var breakInput = document.getElementById('breakDuration');
    if (!breakInput) return;
    var locHome = document.getElementById('locHome');
    if (locHome && locHome.checked) {
        breakInput.value = formatHours(0);
        return;
    }
    var defaultBreak = parseFloat(projectRadio.dataset.defaultBreak || '0');
    breakInput.value = formatHours(defaultBreak);
}

/**
 * Daily form: update break when location changes.
 * HOME → break = 0; OFFICE → break = selected project's default.
 */
function updateBreakForLocation() {
    var breakInput = document.getElementById('breakDuration');
    if (!breakInput) return;
    var locHome = document.getElementById('locHome');
    if (locHome && locHome.checked) {
        breakInput.value = formatHours(0);
        return;
    }
    // Find the selected project radio and use its default break
    var selectedProject = document.querySelector('input[name="projectBtn"]:checked');
    if (selectedProject) {
        var defaultBreak = parseFloat(selectedProject.dataset.defaultBreak || '0');
        breakInput.value = formatHours(defaultBreak);
    }
}

/**
 * Clock session: live timer and break display.
 */
function initClockTimer() {
    var timerEl = document.getElementById('clockTimer');
    if (!timerEl) return;

    var clockInStr = timerEl.dataset.clockIn;
    if (!clockInStr) return;

    var accBreakSeconds = parseInt(timerEl.dataset.breakSeconds || '0', 10);
    var breakStartStr = timerEl.dataset.breakStart;
    var clockInTime = new Date(clockInStr);
    var breakStartTime = breakStartStr ? new Date(breakStartStr) : null;
    var breakDisplayEl = document.getElementById('breakDisplay');

    function updateTimer() {
        var now = new Date();
        var elapsedSeconds = Math.floor((now - clockInTime) / 1000);

        var currentBreakSeconds = accBreakSeconds;
        if (breakStartTime) {
            currentBreakSeconds += Math.floor((now - breakStartTime) / 1000);
        }

        var netSeconds = Math.max(0, elapsedSeconds - currentBreakSeconds);

        var h = Math.floor(netSeconds / 3600);
        var m = Math.floor((netSeconds % 3600) / 60);
        var s = netSeconds % 60;
        timerEl.textContent = h + ':' + (m < 10 ? '0' : '') + m + ':' + (s < 10 ? '0' : '') + s;

        if (breakDisplayEl) {
            var bh = Math.floor(currentBreakSeconds / 3600);
            var bm = Math.floor((currentBreakSeconds % 3600) / 60);
            var bs = currentBreakSeconds % 60;
            breakDisplayEl.textContent = bh + ':' + (bm < 10 ? '0' : '') + bm + ':' + (bs < 10 ? '0' : '') + bs;
        }
    }

    updateTimer();
    setInterval(updateTimer, 1000);
}

document.addEventListener('DOMContentLoaded', function () {
    // Activate Bootstrap tooltips
    var tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltipTriggerList.forEach(function (el) {
        new bootstrap.Tooltip(el);
    });

    // --- Attach blur handlers to all hour inputs ---
    document.querySelectorAll('input.hour-input').forEach(attachHourBlur);

    // --- Attach form submit conversion ---
    document.querySelectorAll('form').forEach(function (form) {
        if (form.querySelector('input.hour-input')) {
            attachFormSubmitConversion(form);
        }
    });

    // --- Hour preset buttons (daily entry + internal activities) ---
    document.querySelectorAll('.hour-preset').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var hoursInput = this.closest('form').querySelector('#hoursWorked');
            if (!hoursInput) hoursInput = this.closest('form').querySelector('input.hour-input');
            if (hoursInput) {
                hoursInput.value = formatHours(parseFloat(this.dataset.hours));
                hoursInput.focus();
            }
        });
    });

    // --- Break preset buttons ---
    document.querySelectorAll('.break-preset').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var breakInput = document.getElementById('breakDuration');
            if (breakInput) {
                breakInput.value = formatHours(parseFloat(this.dataset.hours));
                breakInput.focus();
            }
        });
    });

    // --- Quick description buttons (internal activities) ---
    document.querySelectorAll('.quick-desc').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var descInput = document.getElementById('description');
            if (descInput) {
                descInput.value = this.dataset.desc;
                descInput.focus();
            }
        });
    });

    // --- Clock timer ---
    initClockTimer();

    // --- Format existing values in hour inputs on page load ---
    document.querySelectorAll('input.hour-input').forEach(function (input) {
        if (input.value && input.value.trim()) {
            var val = parseHours(input.value);
            if (!isNaN(val) && val > 0) {
                input.value = formatHours(val);
            }
        }
    });
});
