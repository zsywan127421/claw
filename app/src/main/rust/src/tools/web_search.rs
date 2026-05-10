use std::collections::HashMap;
use super::{Tool, ToolResult};

pub struct WebSearchTool;

impl Tool for WebSearchTool {
    fn name(&self) -> &str { "web_search" }
    fn description(&self) -> &str { "Search the web for information" }

    fn execute(&self, args: HashMap<String, String>) -> ToolResult {
        let query = match args.get("query") {
            Some(q) => q,
            None => return ToolResult {
                success: false,
                output: String::new(),
                error: Some("Missing 'query' argument".to_string()),
            },
        };

        ToolResult {
            success: true,
            output: format!("Web search for: {}\n\nNote: Web search is a premium feature. Please use the DeepSeek API with appropriate tools for production use.", query),
            error: None,
        }
    }
}
