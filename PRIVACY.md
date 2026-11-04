# Pronto Soggiorno — Privacy Policy

*Also available in: [Italiano](PRIVACY.it.md) · [Español](PRIVACY.es.md). The English version prevails.*

**Effective date:** 4 November 2026 · **App version:** 0.1.0 and later

Pronto Soggiorno is a free, open-source Android app that checks the public status page of the
Italian residence-permit portal (`questure.poliziadistato.it`) and notifies you when the status
of your application changes. It is an **independent** project, **not affiliated** with the
Polizia di Stato, the Ministero dell'Interno or Poste Italiane.

## In short
- The app has **no servers, no accounts, no analytics, no advertising and no crash reporting**.
- Your data **stays on your phone**. The developer **never receives** any of it.
- The only external connection the app makes is to `questure.poliziadistato.it`, to check the
  status of the numbers you added.

## What the app stores on your phone
| Data | Why | How it is protected |
|---|---|---|
| Application numbers you add (*pratica* or *assicurata*) | To check their status | Encrypted (AES-256-GCM) with a key kept in the Android Keystore |
| The names you give them (e.g. "Me", "Mum"), optional | To show them instead of the number | Encrypted in the same way |
| History of checks (date, status text published by the portal, collection office, technical error type) | To show changes and detect new ones | Stored in the app's private storage |
| Settings (check frequency, notification preference, whether the welcome screens were seen) | To remember your choices | Stored in the app's private storage |

The app's private storage cannot be read by other apps. **Android backups are disabled** for
this app, so this data is not copied to cloud backups or to a new phone.

## What is sent, and to whom
Each check sends **one HTTPS request** to `https://questure.poliziadistato.it/stranieri/`
containing:
- the application number being checked (this is how the portal works: it is the same request
  your browser makes when you check the status on the website);
- standard technical information that any connection includes: your IP address, the header
  `User-Agent: ProntoSoggiorno/<version>` and the type of content requested.

No name, no alias, no phone identifier and no other data is sent. The app does not store or send
cookies. The portal is operated by the Polizia di Stato (and its technical providers, such as
content-delivery networks); its own privacy policy applies to the requests it receives.

Checks run automatically in the background at the frequency you choose (every 6, 12 or 24 hours)
right after you add a number, and when you tap "Check now". The app checks your numbers one after
another, never in parallel. If the portal does not find a number for 30 days in a row, automatic
checks for it are paused.

Technical safeguards: the app can only connect to `questure.poliziadistato.it` over HTTPS (any
other address is blocked inside the app), unencrypted connections are forbidden and redirects are
not followed.

## Links you choose to open
"Open portal", "Office opening hours" and "Source code" open your **web browser**. The app itself
does not connect to those pages; your browser's privacy policy applies.

## Notifications
Notifications are generated on your phone. They show the name you gave the application, **never
the number**, and the lock-screen version shows only a generic message.

## Permissions
- **Internet**: to contact `questure.poliziadistato.it`.
- **Notifications** (Android 13+, optional): to alert you about changes. The app works without it.
- Technical permissions used by Android's background scheduler (WorkManager), granted automatically
  and not related to your data: network state (to wait for a connection), run after the phone
  restarts (to keep the schedule) and keep the phone awake for the few seconds a check takes.

## How long data is kept
- Routine checks (no change, or failed) are deleted automatically after 60 days.
- Status changes and the latest check of each application are kept until you delete them.
- You can delete one application (with its history) from its screen, or everything from
  **Settings → Delete all data**. Uninstalling the app also deletes all its data.

## Your rights
Because all data is on your device and under your control, you can view, change and delete it at
any time from the app. The developer holds no data about you, so there is nothing to request from
the developer. For data about your application held by the authorities, contact the Polizia di
Stato / your Questura.

## Children
The app is intended for adults dealing with residence-permit procedures. It is not directed at
children.

## Only add numbers you are entitled to check
Add only your own numbers, or numbers whose owner asked you to check them.

## Open source
The full source code is public at <https://github.com/HectorL94/pronto-soggiorno> (GPL-3.0), so
anyone can verify what this policy says.

## Changes to this policy
Changes are published in this file in the repository, with a new effective date. The history of
every change is visible in the repository.

## Contact
Questions or concerns: write to <hlicea.dev@gmail.com> or open an issue at
<https://github.com/HectorL94/pronto-soggiorno/issues>.
