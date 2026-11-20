# Security design

The app requests no broad storage or network permissions. Text files are stored under Android app-private internal storage. The Rust crates forbid unsafe code and use no external crates. The app currently treats all workspace filenames as local files and does not execute content. Save error handling and limits on file size are not implemented. There is no authentication, terminal, cloud execution, or extension loading code. Future terminal/cloud work must define isolation and authentication before command execution is exposed.
