# Pronto Soggiorno — Informativa sulla privacy

*Disponibile anche in: [English](PRIVACY.md) · [Español](PRIVACY.es.md). In caso di differenze prevale la versione inglese.*

**Data di efficacia:** 4 novembre 2026 · **Versione dell'app:** 0.1.0 e successive

Pronto Soggiorno è un'app Android gratuita e open source che consulta la pagina pubblica dello
stato del permesso di soggiorno (`questure.poliziadistato.it`) e ti avvisa quando lo stato della
tua pratica cambia. È un progetto **indipendente**, **non affiliato** alla Polizia di Stato, al
Ministero dell'Interno né a Poste Italiane.

## In breve
- L'app **non ha server, account, statistiche, pubblicità né invio di segnalazioni di errore**.
- I tuoi dati **restano sul tuo telefono**. Lo sviluppatore **non riceve** nessuno di essi.
- L'unica connessione esterna dell'app è verso `questure.poliziadistato.it`, per verificare lo
  stato dei numeri che hai aggiunto.

## Cosa salva l'app sul telefono
| Dato | Perché | Come è protetto |
|---|---|---|
| Numeri che aggiungi (*pratica* o *assicurata*) | Per verificarne lo stato | Cifrati (AES-256-GCM) con una chiave conservata nel Keystore di Android |
| Nomi che dai loro (es. "Io", "Mamma"), facoltativi | Per mostrarli al posto del numero | Cifrati allo stesso modo |
| Cronologia delle verifiche (data, testo pubblicato dal portale, ufficio di ritiro, tipo di errore tecnico) | Per mostrare i cambiamenti e rilevarne di nuovi | Nella memoria privata dell'app |
| Impostazioni (frequenza, preferenze di notifica, schermate di benvenuto già viste) | Per ricordare le tue scelte | Nella memoria privata dell'app |

La memoria privata dell'app non è leggibile da altre app. **I backup di Android sono disattivati**
per questa app: questi dati non vengono copiati nei backup cloud né su un nuovo telefono.

## Cosa viene inviato e a chi
Ogni verifica invia **una richiesta HTTPS** a `https://questure.poliziadistato.it/stranieri/`
che contiene:
- il numero della pratica da verificare (è così che funziona il portale: è la stessa richiesta
  che fa il tuo browser quando consulti lo stato sul sito);
- le informazioni tecniche standard di qualsiasi connessione: il tuo indirizzo IP, l'intestazione
  `User-Agent: ProntoSoggiorno/<versione>` e il tipo di contenuto richiesto.

Non vengono inviati nomi, alias, identificativi del telefono né altri dati. L'app non salva né
invia cookie. Il portale è gestito dalla Polizia di Stato (e dai suoi fornitori tecnici, come le
reti di distribuzione dei contenuti); alle richieste che riceve si applica la sua informativa.

Le verifiche avvengono automaticamente in background con la frequenza scelta (ogni 6, 12 o 24 ore)
subito dopo aver aggiunto un numero e quando tocchi "Verifica ora". L'app verifica i numeri uno dopo
l'altro, mai in parallelo. Se il portale non trova un numero per 30 giorni di seguito, la sua
verifica automatica viene messa in pausa.

Garanzie tecniche: l'app può collegarsi solo a `questure.poliziadistato.it` tramite HTTPS
(qualsiasi altro indirizzo è bloccato all'interno dell'app), le connessioni non cifrate sono
vietate e i reindirizzamenti non vengono seguiti.

## Link che scegli di aprire
"Apri il portale", "Orari dell'ufficio" e "Codice sorgente" aprono il tuo **browser**. L'app non
si collega a quelle pagine; si applica l'informativa del tuo browser.

## Notifiche
Le notifiche sono generate sul telefono. Mostrano il nome che hai dato alla pratica, **mai il
numero**, e sulla schermata di blocco compare solo un messaggio generico.

## Autorizzazioni
- **Internet**: per contattare `questure.poliziadistato.it`.
- **Notifiche** (Android 13+, facoltativa): per avvisarti dei cambiamenti. L'app funziona anche senza.

## Per quanto tempo si conservano i dati
- Le verifiche di routine (senza novità o non riuscite) si cancellano automaticamente dopo 60 giorni.
- I cambi di stato e l'ultima verifica di ogni pratica restano finché non li elimini.
- Puoi eliminare una pratica (con la sua cronologia) dalla sua schermata, oppure tutto da
  **Impostazioni → Cancella tutti i dati**. Disinstallando l'app si cancellano tutti i suoi dati.

## I tuoi diritti
Poiché tutti i dati sono sul tuo dispositivo e sotto il tuo controllo, puoi vederli, modificarli e
cancellarli in qualsiasi momento dall'app. Lo sviluppatore non conserva dati su di te, quindi non
c'è nulla da richiedergli. Per i dati della tua pratica in possesso delle autorità, rivolgiti alla
Polizia di Stato / alla tua Questura.

## Minori
L'app è pensata per adulti che seguono pratiche di soggiorno. Non è rivolta ai minori.

## Aggiungi solo numeri che hai diritto di verificare
Aggiungi solo i tuoi numeri, o quelli che il titolare ti ha chiesto di verificare.

## Open source
Il codice sorgente completo è pubblico su <https://github.com/HectorL94/pronto-soggiorno>
(GPL-3.0): chiunque può verificare quanto dichiarato in questa informativa.

## Modifiche
Le modifiche vengono pubblicate in questo file del repository, con una nuova data di efficacia.
La cronologia di ogni modifica è visibile nel repository.

## Contatti
Domande o segnalazioni: apri una issue su <https://github.com/HectorL94/pronto-soggiorno/issues>.
