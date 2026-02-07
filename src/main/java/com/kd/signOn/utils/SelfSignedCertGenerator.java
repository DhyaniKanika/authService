package com.kd.signOn.utils;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Date;

public class SelfSignedCertGenerator {

    public static X509Certificate generate(String dn, KeyPair keyPair) throws Exception {

        long now = System.currentTimeMillis();

        Date notBefore = new Date(now);
        Date notAfter = new Date(now + (365L * 24 * 60 * 60 * 1000));

        BigInteger serial = BigInteger.valueOf(now);

        X500Name name = new X500Name(dn);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .build(keyPair.getPrivate());

        JcaX509v3CertificateBuilder builder =
                new JcaX509v3CertificateBuilder(
                        name,
                        serial,
                        notBefore,
                        notAfter,
                        name,
                        keyPair.getPublic());

        X509CertificateHolder holder = builder.build(signer);

        return new JcaX509CertificateConverter()
                .getCertificate(holder);
    }
}
