# Debian package
## Requirements
- Any distribution that supports Java 11 or Java 8

## Install
1. Download the [latest release](https://github.com/ma1uta/ma1sd/releases/latest)
2. Run:
```bash
dpkg -i /path/to/downloaded/ma1sd.deb
```
## Files
| Location                            | Purpose                                      |
|-------------------------------------|----------------------------------------------|
| `/etc/ma1sd`                        | Configuration directory                      |
| `/etc/ma1sd/ma1sd.yaml`             | Main configuration file                      |
| `/etc/systemd/system/ma1sd.service` | Systemd configuration file for ma1sd service |
| `/usr/lib/ma1sd`                    | Binaries                                     |
| `/var/lib/ma1sd`                    | Data                                         |
| `/var/lib/ma1sd/signing.key`        | Default location for ma1sd signing keys      |

## Control
Start ma1sd using:
```bash
sudo systemctl start ma1sd
```

Stop ma1sd using:
```bash
sudo systemctl stop ma1sd
```

## Troubleshoot
All logs are sent to `STDOUT` which are saved in `/var/log/syslog` by default.  
You can:
- grep & tail using `ma1sd`:
```
tail -n 99 -f /var/log/syslog | grep ma1sd
```
- use Systemd's journal:
```
journalctl -f -n 99 -u ma1sd
```
