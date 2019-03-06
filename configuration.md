# Configuration Guide

Health APIs run in Docker containers in AWS and are configured via properties files in AWS S3 
buckets. Application properties, certificates, and Kerberos configuration files are copied from
the S3 bucket into containers during the start up process. 

Docker containers must be bootstrapped with environment variables that enable access
to the S3 buckets.

## S3
Buckets are organized as follows:

```
S3
 ├ ${app-name}/application.properties
 ├ ...
 ├ krb5/krb5.conf
 └ system_certs/
   ├ <any>.jks
   ├ <any>-truststore.jks
   └ ...
```

- Application names can be anything. This will be configured per container using the `AWS_APP_NAME`
  environment variable.
- Each application has has an `application.properties` which contain Spring Boot configuration.
- `system_certs` may contain any number or keystore and truststore files.

##### On container start
- Based on the configured application name, the `application.properties` is copied to `/opt/va/` 
  to be loaded by the Spring Boot application.

### Identity Service
```
# HTTPS Server
server.ssl.key-store ...................... Path to keystore, e.g. /opt/va/certs/<any>.jks
server.ssl.key-store-password ............. Password for the keystore
server.ssl.key-alias ...................... Key alias in the keystore to use

# Database
spring.datasource.url ..................... JDBC URL to the identity database
spring.datasource.username ................ Database user name
spring.datasource.password ................ Database password
```

