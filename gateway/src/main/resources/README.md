# SSL

To enable SSL, the following steps are needed

## Configure Keystore

- Generate a .pfx certificate (same one as backend is used for simplicity).
- Configure keystore in application yaml file by adding `ssl.bundle.jks` config
  and using it by setting `server.ssl.bundle: sslbundle`.

## Configure Truststore

- From the .pfx certificate, generate a truststore file
- configure truststore in application yaml

### Generating truststore

The certificate `quiz-me.pfx` contains only `PrivateKeyEntry` and no truststore entries, so we need to generate
truststore from it.
First, convert to `.cert` file

```
keytool -exportcert  \
-keystore quizme-cert.pfx  \
-storetype PKCS12 \
-alias <your alias> \
-file quizme-cert.cer  \
-rfc
```

Then convert it to `.pfx` containing a truststore

```
keytool -importcert \
  -file quizme-cert.cer \
  -keystore quizme-trust.pfx \
  -storetype PKCS12 \
  -alias <your-alias> \
  -storepass <password> \
  -noprompt
```

The `-importcert` automatically adds `trustedCertEntry`. You can check the difference
between the two .pfx files using the command
`keytool -list -keystore your-cert.pfx -storetype PKCS12 -storepass <password>`

## Add ClientHttpRequestFactory bean

Add this bean to trust self-signed certificates (the one we use in our backend).
Otherwise, we get an error when trying to send a request to our backend.