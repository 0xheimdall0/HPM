# HPM — Heimdall's Password Manager

A local, offline password manager for Windows/Linux/macOS. Your vault is encrypted
on disk with **AES-GCM** under a key derived from your master password via **Argon2**,
and the master password is never stored. Built in Java with a Swing UI.

> ⚠️ **Disclaimer:** HPM is a personal/learning project. It has **not** been
> independently security-audited. Use it at your own risk, and do not rely on it as
> your sole store for high-value credentials. Always keep backups of your `vault.dat`.

---

## Features

- **Encrypted vault** — Argon2 key derivation + AES-GCM authenticated encryption, random per-vault salt, versioned file format, atomic (crash-safe) saves.
- **Entries** — add, edit, delete; show, copy, or double-click to copy a password.
- **Password generator** — configurable length and character sets, guarantees character variety, can exclude look-alike characters. Settings persist and act as defaults for new entries.
- **Two-factor (TOTP)** — store 2FA secrets and view live 6-digit codes; import a secret from a **QR image**, or bulk-import all accounts from a **Google Authenticator** export.
- **Search & sort** — live filtering and A–Z / Z–A sorting.
- **Security tools**
  - Weak / low-variety / reused-password check.
  - Breach check against **HaveIBeenPwned** using k-anonymity (only a hash *prefix* is sent).
- **Auto-lock** — after inactivity, on minimize, or on focus loss (all configurable).
- **Brute-force resistance** — configurable failed-attempt lockout with linear or exponential back-off (persists across restarts).
- **Clipboard hygiene** — copied passwords auto-clear after a configurable delay (only if untouched).
- **Master password** — create with confirmation and a strength policy; change it (re-entering the current one).

## Security model

- The vault (`vault.dat`) is encrypted; without your master password it is unreadable.
- The master password is held only as a `char[]` in memory and wiped after deriving the key; only the derived key is kept while unlocked, and it is cleared on lock.
- **There is no recovery.** If you forget the master password, the vault cannot be decrypted.

## Requirements

- To run the packaged app: nothing, the Windows build bundles its own Java runtime.
- To build from source: **JDK 21+** and **Maven**.

## Download & run

Grab the latest release from the Releases page.

- **Windows:** unzip and run `HPM.exe`.
- **Cross-platform JAR:** with Java installed, run `java -jar HPM.jar`.

## Build from source

```bash
git clone https://github.com/0xheimdall0/HPM.git
cd HPM
mvn clean package
java -jar target/HPM-1.0.jar
```

To build the native Windows app image:

```bash
jpackage --type app-image --name HPM --input input \
  --main-jar HPM-1.0.jar --main-class HPMUI --icon input/HPM.ico --dest dist
```

## Usage

1. On first launch, create a master password (you'll be asked to confirm it).
2. Add entries, or import 2FA accounts via QR.
3. Lock the vault (or let auto-lock do it) when you step away.
4. Everything is saved automatically and encrypted on disk.

See the in-app **Help** button (Settings tab) for a full explanation of every option.

## Files created

- `vault.dat` — your encrypted vault (owner-only permissions where supported).
- `settings.properties` — non-secret preferences (auto-lock, clipboard delay, etc.).

Both are created in the working directory and are git-ignored.

## License

Released under the [MIT License](LICENSE).
