function attachPasswordValidation(formId, passId, confirmId, errorId) {
    const form = document.getElementById(formId);
    if (!form) return;

    form.addEventListener("submit", function(e) {
        const pass = document.getElementById(passId).value;
        const confirm = document.getElementById(confirmId).value;
        const err = document.getElementById(errorId);

        err.textContent = "";

        if (!Validation.passwordsMatch(pass, confirm)) {
            err.textContent = "Passwords do not match.";
            e.preventDefault();
            return;
        }

        if (!Validation.isStrongPassword(pass)) {
            err.textContent =
                "Password must be 8–64 characters and must include one upper case letter, one lower case letter, one number, and one of these special character (!@#$%^&*()_+=-)";
            e.preventDefault();
        }
    });
}

function attachCompanyEmailValidation(formId, emailId, errorId) {
    const form = document.getElementById(formId);
    if (!form) return;

    form.addEventListener("submit", function(e) {
        const email = document.getElementById(emailId).value;
        const err = document.getElementById(errorId);

        err.textContent = "";

        if (!Validation.isCompanyEmail(email)) {
            err.textContent = "Email must be a company email (@kd.com)";
            e.preventDefault();
        }
    });
}
