# How it works

The Rust `TextBuffer` owns a `String`. Before slicing or replacing, it checks range order, length, and UTF-8 character boundaries. `Editor` tracks a cursor and optional anchor/head selection. Insertion replaces a selection or inserts at the cursor; each edit stores replaced and inserted text for undo/redo. Search reports byte ranges, and line/column lookup counts lines and Unicode scalar values.

On Android, `MainActivity` creates a Compose screen rooted at an app-private workspace directory. The screen refreshes file names, creates an empty text file, loads a selected file into Compose state, allows direct text editing, and writes the current contents on Save. I/O errors on open are caught and shown in the status message. Save errors are not currently caught and should be addressed before production use.
