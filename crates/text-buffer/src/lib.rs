//! UTF-8 safe storage and primitive edits for the Reverso editor.
use std::ops::Range;

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum BufferError {
    OutOfBounds { index: usize, len: usize },
    NotCharBoundary { index: usize },
    InvalidRange { start: usize, end: usize },
}

/// Owns document text and exposes byte-offset edits only at UTF-8 boundaries.
#[derive(Debug, Clone, Default, PartialEq, Eq)]
pub struct TextBuffer { text: String }

impl TextBuffer {
    pub fn new(text: impl Into<String>) -> Self { Self { text: text.into() } }
    pub fn as_str(&self) -> &str { &self.text }
    pub fn len_bytes(&self) -> usize { self.text.len() }
    pub fn is_empty(&self) -> bool { self.text.is_empty() }
    pub fn is_char_boundary(&self, index: usize) -> bool { self.text.is_char_boundary(index) }

    fn validate(&self, range: &Range<usize>) -> Result<(), BufferError> {
        if range.start > range.end { return Err(BufferError::InvalidRange { start: range.start, end: range.end }); }
        for index in [range.start, range.end] {
            if index > self.text.len() { return Err(BufferError::OutOfBounds { index, len: self.text.len() }); }
            if !self.text.is_char_boundary(index) { return Err(BufferError::NotCharBoundary { index }); }
        }
        Ok(())
    }

    /// Replace a valid byte range, returning the removed text for undo.
    pub fn replace(&mut self, range: Range<usize>, replacement: &str) -> Result<String, BufferError> {
        self.validate(&range)?;
        let removed = self.text[range.clone()].to_owned();
        self.text.replace_range(range, replacement);
        Ok(removed)
    }
    pub fn insert(&mut self, at: usize, text: &str) -> Result<(), BufferError> { self.replace(at..at, text).map(|_| ()) }
    pub fn delete(&mut self, range: Range<usize>) -> Result<String, BufferError> { self.replace(range, "") }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn edits_preserve_multibyte_utf8() {
        let mut b = TextBuffer::new("a🦀c");
        assert_eq!(b.delete(1..5).unwrap(), "🦀");
        b.insert(1, "界").unwrap();
        assert_eq!(b.as_str(), "a界c");
    }
    #[test]
    fn rejects_mid_codepoint_and_out_of_bounds() {
        let mut b = TextBuffer::new("🦀");
        assert_eq!(b.delete(1..2), Err(BufferError::NotCharBoundary { index: 1 }));
        assert_eq!(b.insert(9, "x"), Err(BufferError::OutOfBounds { index: 9, len: 4 }));
    }
}
