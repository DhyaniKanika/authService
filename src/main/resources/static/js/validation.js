window.Validation = {

    isCompanyEmail: email => email.endsWith("@kd.com"),

    passwordsMatch: (p1, p2) => p1 === p2,

    isStrongPassword: p => {
    const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+=-])[A-Za-z\d!@#$%^&*()_+=-]{8,64}$/;
    return regex.test(p);
}
};
