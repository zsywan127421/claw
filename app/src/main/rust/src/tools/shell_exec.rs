use std::collections::HashMap;
use std::process::Command;
use super::{Tool, ToolResult};

pub struct ExecShellTool;

impl Tool for ExecShellTool {
    fn name(&self) -> &str { "exec_shell" }
    fn description(&self) -> &str { "Execute shell command" }

    fn execute(&self, args: HashMap<String, String>) -> ToolResult {
        let command = match args.get("command") {
            Some(c) => c,
            None => return ToolResult {
                success: false,
                output: String::new(),
                error: Some("Missing 'command' argument".to_string()),
            },
        };

        let output = Command::new("sh")
            .arg("-c")
            .arg(command)
            .output();

        match output {
            Ok(out) => {
                let stdout = String::from_utf8_lossy(&out.stdout);
                let stderr = String::from_utf8_lossy(&out.stderr);

                if out.status.success() {
                    ToolResult {
                        success: true,
                        output: if stdout.is_empty() { "Command executed successfully (no output)".to_string() } else { stdout.to_string() },
                        error: None,
                    }
                } else {
                    ToolResult {
                        success: false,
                        output: stdout.to_string(),
                        error: Some(stderr.to_string()),
                    }
                }
            },
            Err(e) => ToolResult {
                success: false,
                output: String::new(),
                error: Some(format!("Failed to execute command: {}", e)),
            },
        }
    }
}
