package com.kd.signOn.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class TlsBootstrap {

    private static final Logger SECURITY_LOG =
            LoggerFactory.getLogger("SECURITY_AUDIT");

    private static final String DEFAULT_PASSWORD = "changeit";
    private static final String DEFAULT_ALIAS = "signon";

    public static void initialize() throws Exception {

        String configured = System.getenv("SERVER_SSL_KEY_STORE");

        if (configured != null && Files.exists(Paths.get(configured))) {
            SECURITY_LOG.info("External TLS configuration detected. Bootstrap skipped.");
            return;
        }

        Path path = Paths.get("keystore/dev-selfsigned.p12");
        Files.createDirectories(path.getParent());

        if (!Files.exists(path)) {
            SECURITY_LOG.warn("No TLS material supplied. Generating development certificate.");

            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair keyPair = kpg.generateKeyPair();

            X509Certificate cert =
                    SelfSignedCertGenerator.generate("CN=localhost", keyPair);

            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, null);
            ks.setKeyEntry(
                    DEFAULT_ALIAS,
                    keyPair.getPrivate(),
                    DEFAULT_PASSWORD.toCharArray(),
                    new java.security.cert.Certificate[]{cert}
            );

            try (OutputStream os = Files.newOutputStream(path)) {
                ks.store(os, DEFAULT_PASSWORD.toCharArray());
            }

            SECURITY_LOG.info("Development TLS certificate generated at {}", path.toAbsolutePath());
        }

        System.setProperty("SERVER_SSL_KEY_STORE", path.toAbsolutePath().toString());
        System.setProperty("SERVER_SSL_KEY_STORE_PASSWORD", DEFAULT_PASSWORD);
        System.setProperty("SERVER_SSL_KEY_STORE_TYPE", "PKCS12");
    }
}
