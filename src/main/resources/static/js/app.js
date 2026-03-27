/* Timesheets — JS for interactive controls */

document.addEventListener('DOMContentLoaded', function () {
    // Activate Bootstrap tooltips
    var tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltipTriggerList.forEach(function (el) {
        new bootstrap.Tooltip(el);
    });

    // --- Hour preset buttons (daily entry + internal activities) ---
    document.querySelectorAll('.hour-preset').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var hoursInput = this.closest('form').querySelector('input[type="number"][id*="hours"], input[type="number"][id="hoursWorked"]');
            if (hoursInput) {
                hoursInput.value = this.dataset.hours;
                hoursInput.focus();
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

    // --- Weekly grid: Fill week button (fill all empty cells in a project row) ---
    document.querySelectorAll('.fill-week-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var projectId = this.dataset.projectId;
            var target = this.dataset.target;
            var inputs = document.querySelectorAll('input[name^="hours_' + projectId + '_"]');
            inputs.forEach(function (input) {
                if (!input.value || input.value === '0' || input.value === '') {
                    input.value = target;
                }
            });
        });
    });

    // --- Weekly grid: Fill day button (fill all empty cells in a column/day) ---
    document.querySelectorAll('.fill-day-btn').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var date = this.dataset.date;
            var inputs = document.querySelectorAll('input[name$="_' + date + '"]');
            inputs.forEach(function (input) {
                if (!input.value || input.value === '0' || input.value === '') {
                    // Find the project's target from the fill-week button in the same row
                    var row = input.closest('tr');
                    var fillWeekBtn = row ? row.querySelector('.fill-week-btn') : null;
                    var target = fillWeekBtn ? fillWeekBtn.dataset.target : '8';
                    input.value = target;
                }
            });
        });
    });
});
