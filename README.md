# HTTPS-MITM-Proxy
Proxy designed for blocking ads, and automatically modifying some http or https communications.

# Usage
rootCA.crt, rootCA.key and key.key must all be in the folder CAroot, alongside the execution location of the proxy server. If the server is a jar file, this would be in the same folder as the jar.

BlockUsers.txt in the same folder as execution may contain any IP addresses that are not allowed to use the proxy, toblock.txt and toblock2.txt list any domains that should be blocked as ads. (one entry per line)

rootCA.crt is the public root certificate, and it must be installed on any devices using the proxy. Installation is different for each operating system, on iOS the certificate can be sent most easily over either airdrop, or by accessing a webpage that hosts the certificate, then you must go to the settings app, install the certificate and enter your password. After the certificate is installed it must be trusted under Settings>General>About>Certificate Trust Settings before it can be used on iOS.

Generate the root certificate and key for the server to use. (Run in the CAroot folder with the included config.)

`openssl req -x509 -sha256 -nodes -newkey rsa:2048 -keyout rootCA.key -out rootCA.crt -days 3650 -config config`

Generate the shared private key for all of the site specific certificates.

`openssl genrsa -out key.key 2048`

The proxy is hosted at localhost:7777 unless changed.

Build with Gson 2.6.2+ for duolingo ios app features.

Requires the linux faketime utility and openssl on the path, on MacOSX the proxy will also use faketime installed by homebrew. If the proxy is hosted on MacOSX it may not work correctly with System Integrity Protection enabled, as it prevents faketime from working correctly.

When running as a jar from the command line, it is recommended that you run the command in the same folder as the CAroot folder and other required files.



