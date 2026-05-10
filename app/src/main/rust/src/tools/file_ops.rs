use std::collections::HashMap;
use super::{Tool, ToolResult};

pub struct FileReadTool;
pub struct FileWriteTool;
pub struct ListDirTool;

impl Tool for FileReadTool {
    fn name(&self) -> &str { "read_file" }
    fn description(&self) -> &str { "Read contents of a file" }

    fn execute(&self, args: HashMap<String, String>) -> ToolResult {
        let path = match args.get("path") {
            Some(p) => p,
            None => return ToolResult {
                success: false,
                output: String::new(),
                error: Some("Missing 'path' argument".to_string()),
            },
        };

        match std::fs::read_to_string(path) {
            Ok(content) => ToolResult {
                success: true,
                output: content,
                error: None,
            },
            Err(e) => ToolResult {
                success: false,
                output: String::new(),
                error: Some(format!("Failed to read file: {}", e)),
            },
        }
    }
}

impl Tool for FileWriteTool {
    fn name(&self) -> &str { "write_file" }
    fn description(&self) -> &str { "Write content to a file" }

    fn execute(&self, args: HashMap<String, String>) -> ToolResult {
        let path = match args.get("path") {
            Some(p) => p,
            None => return ToolResult {
                success: false,
                output: String::new(),
                error: Some("Missing 'path' argument".to_string()),
            },
        };

        let content = match args.get("content") {
            Some(c) => c,
            None => return ToolResult {
                success: false,
                output: String::new(),
                error: Some("Missing 'content' argument".to_string()),
            },
        };

        if let Some(parent) = std::path::Path::new(path).parent() {
            let _ = std::fs::create_dir_all(parent);
        }

        match std::fs::write(path, content) {
            Ok(_) => ToolResult {
                success: true,
                output: format!("File written: {}", path),
                error: None,
            },
            Err(e) => ToolResult {
                success: false,
                output: String::new(),
                error: Some(format!("Failed to write file: {}", e)),
            },
        }
    }
}

impl Tool for ListDirTool {
    fn name(&self) -> &str { "list_dir" }
    fn description(&self) -> &str { "List directory contents" }

    fn execute(&self, args: HashMap<String, String>) -> ToolResult {
        let path = args.get("path")
            .map(|p| p.as_str())
            .unwrap_or(".");

        let mut output = String::new();

        match std::fs::read_dir(path) {
            Ok(entries) => {
                for entry in entries.flatten() {
                    let name = entry.file_name().to_string_lossy().to_string();
                    let file_type = if entry.file_type().map(|t| t.is_dir()).unwrap_or(false) {
                        "[DIR]"
                    } else {
                        "[FILE]"
                    };
                    output.push_str(&format!("{} {}\n", file_type, name));
                }
                ToolResult {
                    success: true,
                    output,
                    error: None,
                }
            },
            Err(e) => ToolResult {
                success: false,
                output: String::new(),
                error: Some(format!("Failed to list directory: {}", e)),
            },
        }
    }
}
