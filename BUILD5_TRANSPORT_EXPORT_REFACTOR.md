# Build5 Transport & Export Refactor

Changes:
- Transport selection is based on runtime Google Wear capability.
- No device brand checks.
- China compatibility transport represents non-GMS Wear environments.
- Export pipeline keeps Watch/Mobile separation.

Next:
- Add mobile import receiver.
- Add Download export through MediaStore.
- Add sync diagnostics.
