//! Platform-independent editing primitives. Android rendering and JNI are separate layers.
use reverso_text_buffer::{BufferError, TextBuffer};
use std::ops::Range;

#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub struct Cursor {
    pub offset: usize,
}
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct Selection {
    pub anchor: usize,
    pub head: usize,
}
#[derive(Debug, Clone, PartialEq, Eq)]
pub enum EditError {
    Buffer(BufferError),
    NoDocument,
}
impl From<BufferError> for EditError {
    fn from(e: BufferError) -> Self {
        Self::Buffer(e)
    }
}

#[derive(Debug, Clone)]
struct Edit {
    range: Range<usize>,
    before: String,
    after: String,
}

/// Editor state for one document, with byte offsets constrained to UTF-8 boundaries.
#[derive(Debug, Clone)]
pub struct Editor {
    buffer: TextBuffer,
    cursor: Cursor,
    selection: Option<Selection>,
    undo: Vec<Edit>,
    redo: Vec<Edit>,
}
impl Editor {
    pub fn new(text: impl Into<String>) -> Self {
        Self {
            buffer: TextBuffer::new(text),
            cursor: Cursor::default(),
            selection: None,
            undo: vec![],
            redo: vec![],
        }
    }
    pub fn text(&self) -> &str {
        self.buffer.as_str()
    }
    pub fn cursor(&self) -> Cursor {
        self.cursor
    }
    pub fn selection(&self) -> Option<&Selection> {
        self.selection.as_ref()
    }
    pub fn set_cursor(&mut self, offset: usize) -> Result<(), EditError> {
        self.buffer.validate_offset(offset)?;
        self.cursor.offset = offset;
        self.selection = None;
        Ok(())
    }
    pub fn set_selection(&mut self, anchor: usize, head: usize) -> Result<(), EditError> {
        self.buffer
            .validate_range(&(anchor.min(head)..anchor.max(head)))?;
        self.selection = Some(Selection { anchor, head });
        self.cursor.offset = head;
        Ok(())
    }
    pub fn insert(&mut self, text: &str) -> Result<(), EditError> {
        let range = if let Some(s) = self.selection.take() {
            s.anchor.min(s.head)..s.anchor.max(s.head)
        } else {
            self.cursor.offset..self.cursor.offset
        };
        self.apply(range, text)?;
        Ok(())
    }
    pub fn delete(&mut self, range: Range<usize>) -> Result<(), EditError> {
        self.apply(range, "")?;
        Ok(())
    }
    fn apply(&mut self, range: Range<usize>, after: &str) -> Result<(), EditError> {
        let before = self.buffer.replace(range.clone(), after)?;
        self.cursor.offset = range.start + after.len();
        self.selection = None;
        self.undo.push(Edit {
            range,
            before,
            after: after.to_owned(),
        });
        self.redo.clear();
        Ok(())
    }
    pub fn undo(&mut self) -> Result<bool, EditError> {
        if let Some(e) = self.undo.pop() {
            let end = e.range.start + e.after.len();
            self.buffer.replace(e.range.start..end, &e.before)?;
            self.cursor.offset = e.range.start;
            self.redo.push(e);
            Ok(true)
        } else {
            Ok(false)
        }
    }
    pub fn redo(&mut self) -> Result<bool, EditError> {
        if let Some(e) = self.redo.pop() {
            let end = e.range.start + e.before.len();
            self.buffer.replace(e.range.start..end, &e.after)?;
            self.cursor.offset = e.range.start + e.after.len();
            self.undo.push(e);
            Ok(true)
        } else {
            Ok(false)
        }
    }
    /// 0-based line and Unicode scalar column at a UTF-8 byte offset.
    pub fn line_column(&self, offset: usize) -> Result<(usize, usize), EditError> {
        self.buffer.validate_offset(offset)?;
        let prefix = &self.text()[..offset];
        let line = prefix.bytes().filter(|b| *b == b'\n').count();
        let col = prefix.rsplit('\n').next().unwrap_or("").chars().count();
        Ok((line, col))
    }
    pub fn find(&self, needle: &str, from: usize) -> Option<Range<usize>> {
        if needle.is_empty() || from > self.text().len() || !self.text().is_char_boundary(from) {
            return None;
        }
        self.text()[from..]
            .find(needle)
            .map(|i| from + i..from + i + needle.len())
    }
    pub fn replace_all(&mut self, needle: &str, replacement: &str) -> Result<usize, EditError> {
        if needle.is_empty() {
            return Ok(0);
        }
        let hits: Vec<_> = self
            .text()
            .match_indices(needle)
            .map(|(i, s)| i..i + s.len())
            .collect();
        for range in hits.iter().rev() {
            self.apply(range.clone(), replacement)?;
        }
        Ok(hits.len())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn edit_undo_redo_round_trip() {
        let mut e = Editor::new("hello");
        e.set_cursor(5).unwrap();
        e.insert(" world").unwrap();
        assert_eq!(e.text(), "hello world");
        assert!(e.undo().unwrap());
        assert_eq!(e.text(), "hello");
        assert!(e.redo().unwrap());
        assert_eq!(e.text(), "hello world");
    }
    #[test]
    fn selection_insert_replaces_range() {
        let mut e = Editor::new("abcd");
        e.set_selection(1, 3).unwrap();
        e.insert("X").unwrap();
        assert_eq!(e.text(), "aXd");
    }
    #[test]
    fn line_and_column_use_unicode_scalars() {
        let e = Editor::new("🦀x\ny");
        assert_eq!(e.line_column(4).unwrap(), (0, 1));
        assert_eq!(e.line_column(6).unwrap(), (1, 0));
    }
    #[test]
    fn search_replace() {
        let mut e = Editor::new("one one");
        assert_eq!(e.find("one", 1), Some(4..7));
        assert_eq!(e.replace_all("one", "two").unwrap(), 2);
        assert_eq!(e.text(), "two two");
    }
}
