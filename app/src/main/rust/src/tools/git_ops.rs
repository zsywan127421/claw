use std::collections::HashMap;
use std::process::Command;
use super::{Tool, ToolResult};

pub struct GitStatusTool;

impl Tool for GitStatusTool {
    fn name(&self) -> &str { "git_status" }
    fn description(&self) -> &str { "Show git repository status" }

    fn execute(&self, args: HashMap<String, String>) -> ToolResult {
        let path = args.get("path")
            .map(|p| p.as_str())
            .unwrap_or(".");

        let output = Command::new("git")
            .args(&["-C", path, "status", "--porcelain"])
            .output();

        match output {
            Ok(out) => {
                let stdout = String::from_utf8_lossy(&out.stdout);
                ToolResult {
                    success: out.status.success(),
                    output: if stdout.is_empty() { "No changes".to_string() } else { stdout.to_string() },
                    error: None,
                }
            },
            Err(e) => ToolResult {
                success: false,
                output: String::new(),
                error: Some(format!("Not a git repository or git not installed: {}", e)),
            },
        }
    }
}
