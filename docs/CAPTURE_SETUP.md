# Interception setup — capturing a stock (non‑jailbroken) iPhone app's API

This documents how the KIMEP Mobile API was reverse‑engineered so the process can be
repeated (for another app, or if KIMEP changes something). Everything here was done on
macOS with a **stock iPhone** — no jailbreak.

Short version: run mitmproxy on the Mac, point the iPhone's Wi‑Fi proxy at it, trust the
mitmproxy CA. If the app ignores the proxy (common for Flutter/React‑Native apps), fall
back to a **DNS redirect + reverse proxy** so the app connects to the Mac directly.

---

## 0. Prerequisites

```bash
brew install mitmproxy poppler          # mitmproxy for MITM, poppler for pdftotext
brew install libimobiledevice ideviceinstaller   # USB device tooling (optional)
```

Find the Mac's LAN IP (the iPhone must be on the same Wi‑Fi):

```bash
ipconfig getifaddr en0        # e.g. 192.168.10.5
```

---

## 1. Baseline: HTTPS interception via the system proxy

Start a capture proxy. `capture.py` in this repo logs every request/response to
`captures/traffic.jsonl`:

```bash
mitmdump -s capture.py -p 8080 --set block_global=false
```

On the **iPhone**:

1. `Settings → Wi‑Fi → (i) → Configure Proxy → Manual`
   - Server: `<Mac LAN IP>`, Port: `8080`, Authentication off.
2. In **Safari** (not Chrome) open `http://mitm.it`, tap the Apple/iOS icon, install the
   profile.
3. `Settings → General → VPN & Device Management` → install the mitmproxy profile.
4. `Settings → General → About → Certificate Trust Settings` → enable **Full Trust** for
   mitmproxy. *(Easy to miss; without it every HTTPS request fails with "connection is
   not secure".)*

Sanity check: `https://example.com` loads without a warning.

> **If the app is HTTP (not HTTPS) you're done immediately.**
> **If the app pins its certificate**, you'll see repeated `Client TLS handshake failed`
> for the app's host in mitmproxy and no data. That needs Frida on a jailbroken device —
> out of scope here.

---

## 2. Detect an app that ignores the system proxy

Flutter (`user-agent: Dart/2.16 (dart:io)`) and many React‑Native apps use their own
network stack and **bypass the iOS system proxy entirely** — mitmproxy sees nothing.

Symptoms: the proxy log shows only Apple/Google telemetry, no app traffic, and no TLS
handshake failures either.

Two USB‑based ways to find the real backend host:

**a) Device syslog** (sometimes leaks URLs):

```bash
idevicesyslog > captures/syslog.txt 2>&1 &
# use the app, then:
grep -aoE 'https?://[A-Za-z0-9._~:/?#@!$&*+,;=%-]+' captures/syslog.txt | sort -u
```

**b) Packet capture over USB** — sees *everything*, proxy or not:

```bash
rvictl -s <UDID>                       # creates interface rvi0
sudo tcpdump -i rvi0 -n -s 0 -w captures/app.pcap
# use the app, stop tcpdump, then extract TLS SNI (plaintext in the ClientHello):
sudo tcpdump -r captures/app.pcap -n -A 'tcp port 443' \
  | grep -aoE '[a-zA-Z0-9.-]+\.(kz|com|net|org|ru|io)' | sort | uniq -c | sort -rn
rvictl -x <UDID>
```

`idevice_id -l` lists the UDID. `ideviceinstaller list` lists installed bundle IDs.

For KIMEP Mobile (`kz.kimep.kimepmobile`) this revealed the host **`www.kimep.kz`** and,
from syslog, the path `/ext/mobile/…`.

---

## 3. MITM a proxy‑bypassing app: DNS redirect + reverse proxy

Point the app's host at the Mac via DNS, then run mitmproxy in **reverse mode** on 443.
The iPhone's traffic to `www.kimep.kz` then hits the Mac, mitmproxy presents a forged
cert (signed by the already‑trusted CA) and forwards to the real server.

**dnsmasq** (`dnsmasq.conf` in this repo) — answer only the target host with the Mac IP,
forward everything else:

```conf
port=53
listen-address=192.168.10.5
bind-interfaces
address=/www.kimep.kz/192.168.10.5
no-resolv
server=8.8.8.8
server=1.1.1.1
log-queries
log-facility=/Users/artchsh/Desktop/kimep-reverse/captures/dnsmasq.log
```

```bash
sudo /opt/homebrew/opt/dnsmasq/sbin/dnsmasq -C dnsmasq.conf
```

**Reverse proxy on 443** (needs root to bind the privileged port):

```bash
sudo mitmdump --mode reverse:https://www.kimep.kz -p 443 -s capture.py
```

On the iPhone: `Settings → Wi‑Fi → (i) → Configure DNS → Manual` → add only the Mac IP.
Then **force‑quit and reopen** the app (drops pooled connections).

Verify the forged cert from the Mac:

```bash
openssl s_client -connect 127.0.0.1:443 -servername www.kimep.kz </dev/null \
  | openssl x509 -noout -subject -issuer     # subject CN=*.kimep.kz, issuer CN=mitmproxy
```

### Pitfalls learned here

- **DNS loop:** if the Mac's own resolver pointed at the Mac, mitmproxy's upstream lookup
  of `www.kimep.kz` would resolve back to itself. The Mac used the router/ISP resolvers,
  so it was fine — but check `scutil --dns` before trusting this.
- **Apex vs www:** the app used `www.kimep.kz` only. Reverse mode serves a cert for the
  single upstream host, so redirecting the apex too would cause a name mismatch.
- **Certificate pinning** would still defeat this (you'd see handshake failures for the
  pinned host).
- **Alternative if DNS can't be changed:** mitmproxy has a WireGuard mode
  (`mitmdump --mode wireguard`) — install the WireGuard app on the phone, import
  `~/.mitmproxy/wireguard.conf` (scan the QR), connect. It tunnels *all* traffic through
  mitmproxy and is immune to proxy‑bypassing clients.
- The reverse proxy forges a cert for the upstream hostname; because the CA is trusted
  and the app did not pin, the Flutter client accepted it.

---

## 4. Teardown

```bash
pkill -f mitmdump
sudo pkill -f mitmdump          # the :443 instance runs as root
sudo pkill dnsmasq
pkill -f idevicesyslog
rvictl -x <UDID>                # remove the USB capture interface
```

Then restore the **iPhone**:

- `Settings → Wi‑Fi → (i) → Configure Proxy → Off`
- `Settings → Wi‑Fi → (i) → Configure DNS → Automatic`
- Optionally remove the mitmproxy profile: `Settings → General → VPN & Device Management`

Confirm ports are free: `lsof -nP -iTCP:53,443,8080 -sTCP:LISTEN`.

---

## 5. Files in this repo

| Path | Purpose |
|---|---|
| `capture.py` | mitmproxy addon; logs flows to `captures/traffic.jsonl` |
| `dnsmasq.conf` | DNS redirect used for the reverse‑proxy phase |
| `captures/traffic.jsonl` | every captured request/response |
| `captures/bodies/` | one file per endpoint with full headers + body |
| `docs/KIMEP_Mobile_API.md` | the resulting API reference |
| `tools/parse_calendar.py` | parses the academic‑calendar PDFs into JSON |

## 6. Post‑capture tips

- Probe the server directly once you know the host — it may be plain ASP.NET/IIS and
  answer without auth (`curl -sSI https://www.kimep.kz/ext/mobile/`).
- Watch response headers: `Server`, `X-Powered-By`, `X-AspNet-Version` identify the stack
  and hint at the URL scheme.
- Dates came back as ASP.NET JSON dates `"\/Date(ms)\/"`; see the API doc for the two
  encodings (absolute dates use UTC+5, class times use the legacy UTC+6).
