# Original body-map backup

`MuscleBodyArt.kt` and `MuscleBodyDiagram.kt` are the original files from commit
`6c56ab2fa4502c62cfd9a85c8cd191fef42e93b0`. `manifest.json` records their paths and SHA-256 hashes.
They are kept outside Android source directories and are not included in the app.

To inspect the backup without changing anything:

```powershell
python docs/design/bodymap/original-backup/restore.py
```

To restore the original body map when requested:

```powershell
python docs/design/bodymap/original-backup/restore.py --restore
```

The restore script first saves the current artwork, renderer, and upgrade-specific tests in a
dated folder here. It then restores the two original source files and moves the two new tests
out of the source roots (they require the new geometry format). It never resets the repository,
touches workout data, changes other app files, or installs an APK. Rebuild and verify before
installing the restored version. Do not run the new SVG generator after restoring: that would
replace the old art again.
