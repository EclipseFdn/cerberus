# Monitoring tools for statuspage.io

## Configuration

statuspage.io.json
```json
{
  "statuspage.io": [
    {
      "url": "https://api.statuspage.io/v1/",
      "page_id": "page_id",
      "token": "oauth_token",
      "fetch_rate": "PT3M"
    }
  ]
}
```

monitors.json
```json
{
  "monitors": {
    "http_status": [
      {
        "component_name": "google.com",
        "target": "https://www.google.com"
      },
      {
        "component_name": "amazon.com",
        "target": "https://www.amazon.com",
        "method": "GET",
    
        "timeout": "PT60S",
        
        "monitoring_history": "PT30M",
        "initial_delay": "PT30S",
        "period": "PT5S",

        "anomalies_detection": {
          "degraded_performance_threshold": 5,
          "partial_outage_threshold": 10,
          "major_outage_threshold": 20,
          "period": "PT1M",
          "initial_delay": "PT1M"
        }
      }
    ]
  },

  "default_configuration": {
    "http_status": {
      "method": "GET",
      
      "status_code_min": 200,
      "status_code_max": 399,
      "timeout": "PT20S",
      
      "monitoring_history": "PT10M",
      "initial_delay": "PT0S",
      "period": "PT15S",

      "anomalies_detection": {
        "degraded_performance_threshold": 1,
        "partial_outage_threshold": 3,
        "major_outage_threshold": 5,
        "period": "PT1M",
        "initial_delay": "PT1M"
      }
    }
  }
}
```

## Install from source (CentOS 8+)

```bash
sudo dnf install git make jq epel-release
sudo dnf install xmlstarlet
sudo git clone https://github.com/EclipseFdn/cerberus.git /usr/local/cerberus
sudo chown -R webmaster:webmaster /usr/local/cerberus
export TMPDIR=${TMPDIR:-/tmp}
make -C /usr/local/cerberus relocatable-cerberus
curl -sSL -o /usr/local/cerberus/monitors.json https://github.com/EclipseFdn/status.eclipse.org/raw/master/monitors.json
vim /usr/local/cerberus/statuspage.io.json
chmod 600 /usr/local/cerberus/statuspage.io.json
```

/etc/systemd/system/cerberus.service
```
[Unit]
Description=Cerberus Monitoring tools 
After=network-online.target
 
[Service]
Type=simple

User=webmaster
Group=webmaster
UMask=007
 
ExecStart=/usr/local/cerberus/target/cerberus/bin/cerberus -c /usr/local/cerberus/monitors.json -s /usr/local/cerberus/statuspage.io.json
 
Restart=on-failure
 
# Configures the time to wait before service is stopped forcefully.
TimeoutStopSec=300
 
[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable cerberus.service
sudo systemctl start cerberus.service
sudo journalctl -u cerberus.service -f
```

/etc/systemd/system/cerberus-restarter.service
```
[Unit]
Description=Cerberus Restarter
After=network.target

[Service]
Type=oneshot
ExecStart=/usr/bin/systemctl restart cerberus.service

[Install]
WantedBy=multi-user.target
```

/etc/systemd/system/cerberus-restarter.path
```
[Path]
PathModified=/usr/local/cerberus/monitors.json
PathModified=/usr/local/cerberus/statuspage.io.json

[Install]
WantedBy=multi-user.target
```

```bash
sudo systemctl enable cerberus-restarter.{path,service}
sudo systemctl start cerberus-restarter.{path,service}
sudo crontab -e 
```

crontab
```
*/3 * * * *     . /usr/local/cerberus/download.sh && download_ifmodified https://github.com/EclipseFdn/status.eclipse.org/raw/master/monitors.json /usr/local/cerberus/monitors.json
```

## Building

### Requirement

```bash
$ sudo dnf install make java-11-openjdk-devel
```

### Uberjar

```bash
$ make uberjar
```

### Natives

#### Relocatable application folder

```bash
$ make relocatable-cerberus
```

#### Single binary

```bash
$ make native-cerberus
```

### Check dependencies 

```bash
$ make display-updates
```

# Trademarks

Eclipse® is a Trademark of the Eclipse Foundation, Inc.
Eclipse Foundation is a Trademark of the Eclipse Foundation, Inc.

## Copyright and license

Copyright 2020 the [Eclipse Foundation, Inc.](https://www.eclipse.org) and the [cerberus authors](https://github.com/eclipsefdn/cerberus/graphs/contributors). Code released under the [Eclipse Public License Version 2.0 (EPL-2.0)](https://github.com/eclipsefdn/cerberus/blob/src/LICENSE).