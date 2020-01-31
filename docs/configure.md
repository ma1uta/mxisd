# Configuration
- [Concepts](#concepts)
  - [Syntax](#syntax)
- [Matrix](#matrix)
- [Server](#server)
- [Storage](#storage)
- [Identity stores](#identity-stores)
- [3PID Validation sessions](#3pid-validation-sessions)
- [Notifications](#notifications)

## Concepts
### Syntax
The configuration file is [YAML](http://yaml.org/) based:
```yaml
my:
  config:
    item: 'value'

```

When referencing keys in all documents, a property-like shorthand will be used. The shorthand for the above example would be `my.config.item`

## Matrix
`matrix.domain`
Matrix domain name, same as the Homeserver, used to build appropriate Matrix IDs |

---

`matrix.identity.servers`
Namespace to create arbitrary list of Identity servers, usable in other parts of the configuration |

Example:
```yaml
matrix:
  identity:
    servers:
      myOtherServers:
        - 'https://other1.example.org'
        - 'https://other2.example.org'
```
Create a list under the label `myOtherServers` containing two Identity servers: `https://other1.example.org` and `https://other2.example.org`.

## Server
- `server.name`: Public hostname of ma1sd, if different from the Matrix domain.
- `server.port`: HTTP port to listen on (unencrypted)
- `server.publicUrl`: Defaults to `https://{server.name}`

## Unbind (MSC1915)
- `session.policy.unbind.enabled`: Enable or disable unbind functionality (MSC1915). (Defaults to true).

## Hash lookups, Term and others (MSC2140, MSC2134)
See the [dedicated document](MSC2140_MSC2134.md) for configuration.

*Warning*: Unbind check incoming request by two ways:
- session validation.
- request signature via `X-Matrix` header and uses `server.publicUrl` property to construct the signing json;
Commonly the `server.publicUrl` should be the same value as the `trusted_third_party_id_servers` property in the synapse config.

## Storage
### SQLite
```yaml
storage:
  backend: sqlite # default
  provider:
    sqlite:
      database: /var/lib/ma1sd/store.db #  Absolute location of the SQLite database
```

### Postgresql
```yaml
storage:
  backend: postgresql
  provider:
    postgresql:
      database: //localhost:5432/ma1sd
      username: ma1sd
      password: secret_password
```
See [the migration instruction](migration-to-postgresql.md) from sqlite to postgresql


## Logging
```yaml
logging:
  root: error  # default level for all loggers (apps and thirdparty libraries)
  app: info    # log level only for the ma1sd
```

Possible value: `trace`, `debug`, `info`, `warn`, `error`, `off`.

Default value for root level: `info`.

Value for app level can be specified via `MA1SD_LOG_LEVEL` environment variable, configuration or start options.

Default value for app level: `info`.

| start option | equivalent configuration |
| --- | --- |
|  | app: info |
| -v | app: debug |
| -vv | app: trace |

## Identity stores
See the [Identity stores](stores/README.md) for specific configuration

## 3PID Validation sessions
See the dedicated documents:
- [Flow](threepids/session/session.md)
- [Branding](threepids/session/session-views.md)

## Notifications
- `notification.handler.<3PID medium>`: Handler to use for the given 3PID medium. Repeatable.

Example:
```yaml
notification:
  handler:
    email: 'sendgrid'
    msisdn: 'raw'
```
- Emails notifications would use the `sendgrid` handler, which define its own configuration under `notification.handlers.sendgrid`
- Phone notification would use the `raw` handler, basic default built-in handler in ma1sd

### Handlers
- `notification.handers.<handler ID>`: Handler-specific configuration for the given handler ID. Repeatable.

Example:
```yaml
notification:
  handlers:
    raw: ...
    sendgrid: ...
```

Built-in:
- [Raw](threepids/notification/basic-handler.md)
- [SendGrid](threepids/notification/sendgrid-handler.md)
