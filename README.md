# downloader

Android download service abstraction backed by OkHttp and coroutines.

## Owns

- Download request and status models.
- Download manager contract and default implementation.
- File-name resolution helpers.
- Optional Hilt binding for the default downloader.

## Boundaries

- Do not add app-specific download destinations, auth policy, or notification UI.
- Do not use exceptions for expected download failures; report them through `DownloadStatus.Failed`.
- Keep long-running scheduling policy separate unless explicitly owned here.

## Usage

Use this module when a reusable library or app needs queued file download behavior with observable status.
