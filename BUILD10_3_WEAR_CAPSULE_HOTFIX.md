# Build 10.3 Wear capsule / export hotfix

- Shortened the in-class remaining-time label for small round screens.
- Phone export now verifies that a connected Wear node exists before sending. A disconnected phone produces an error and keeps the export screen open instead of reporting false success / returning home.
- Successful phone export also keeps the export screen open.
- Added two independent complication providers: Current Course and Next Course. They can be selected in compatible Samsung/Wear OS complication slots, including Samsung multi-info surfaces that expose complication-provider capsules.
- Refreshed About feature/changelog/developer copy.
