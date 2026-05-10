pub mod file_ops;
pub mod shell_exec;
pub mod git_ops;
pub mod web_search;

use std::collections::HashMap;

#[derive(Debug, Clone)]
pub struct ToolResult {
    pub success: bool,
    pub output: String,
    pub error: Option<String>,
}

pub struct ToolRegistry {
    tools: HashMap<String, Box<dyn Tool>>,
}

trait Tool: Send + Sync {
    fn name(&self) -> &str;
    fn description(&self) -> &str;
    fn execute(&self, args: HashMap<String, String>) -> ToolResult;
}

impl ToolRegistry {
    pub fn new() -> Self {
        let mut tools = HashMap::new();
        tools.insert("read_file".to_string(), Box::new(file_ops::FileReadTool) as Box<dyn Tool>);
        tools.insert("write_file".to_string(), Box::new(file_ops::FileWriteTool) as Box<dyn Tool>);
        tools.insert("list_dir".to_string(), Box::new(file_ops::ListDirTool) as Box<dyn Tool>);
        tools.insert("exec_shell".to_string(), Box::new(shell_exec::ExecShellTool) as Box<dyn Tool>);
        tools.insert("git_status".to_string(), Box::new(git_ops::GitStatusTool) as Box<dyn Tool>);
        tools.insert("web_search".to_string(), Box::new(web_search::WebSearchTool) as Box<dyn Tool>);
        Self { tools }
    }

    pub fn execute(&self, name: &str, args: HashMap<String, String>) -> ToolResult {
        if let Some(tool) = self.tools.get(name) {
            tool.execute(args)
        } else {
            ToolResult {
                success: false,
                output: String::new(),
                error: Some(format!("Tool '{}' not found", name)),
            }
        }
    }

    pub fn list_tools(&self) -> Vec<String> {
        self.tools.keys().cloned().collect()
    }
}
