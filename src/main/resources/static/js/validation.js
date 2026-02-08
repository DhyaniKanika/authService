window.Validation = {

    isCompanyEmail: email => email.endsWith("@kd.com"),

    passwordsMatch: (p1, p2) => p1 === p2,

    isStrongPassword: p => {
        const regex = /^[A-Za-z0-9!@#$%^&*()_+=-]{8,64}$/;
        return regex.test(p);
    }
};
