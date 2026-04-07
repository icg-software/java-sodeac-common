# java-sodeac-common

Helper tools and core framework - incubator

## Installation

- runs with Apache Karaf 4.4.10 → OSGi 8.0.0
- `mvn clean install`

### via Karaf commands

```bash
# open karaf console
karaf
```

```bash

# Prerequisites
install -s mvn:org.osgi/org.osgi.service.component/1.5.1
feature:install scr log

install -s mvn:org.osgi/org.osgi.service.component.annotations/1.5.1

# --- project bundle
install -s mvn:org.sodeac/org.sodeac.common/2.0.0-SNAPSHOT

# Enable auto-repackage for SNAPSHOT bundles.
#bundle:watch org.sodeac.common
```
