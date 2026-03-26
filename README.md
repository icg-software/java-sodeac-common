# java-sodeac-common

Helper tools and core framework - incubator

## Installation

- runs with Apache Karaf 4.4.10 -> OSGi 8.0.0
- `mvn clean install`

### Karaf

#### Debugging

```bash
# open karaf console in debug mode
karaf debug
```

```bash

# Prerequisites
install -s mvn:org.osgi/org.osgi.service.component/1.5.1
feature:install scr

install -s mvn:org.osgi/org.osgi.service.component.annotations/1.5.1

# --- project bundle
install -s mvn:org.sodeac/org.sodeac.common/2.0.0-SNAPSHOT

bundle:watch org.sodeac.common
```
