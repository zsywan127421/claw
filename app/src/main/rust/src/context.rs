use std::collections::VecDeque;

pub struct ContextManager {
    messages: VecDeque<Message>,
    max_tokens: usize,
}

#[derive(Debug, Clone)]
pub struct Message {
    pub role: String,
    pub content: String,
}

impl ContextManager {
    pub fn new(max_tokens: usize) -> Self {
        Self { messages: VecDeque::new(), max_tokens }
    }

    pub fn add_message(&mut self, role: &str, content: &str) {
        self.messages.push_back(Message { role: role.to_string(), content: content.to_string() });
    }

    pub fn get_messages(&self) -> Vec<crate::api::Message> {
        self.messages.iter().map(|m| crate::api::Message { role: m.role.clone(), content: m.content.clone() }).collect()
    }

    pub fn clear(&mut self) { self.messages.clear(); }
}

pub fn create_system_prompt(mode: &str) -> String {
    match mode {
        "plan" => "你是一个代码分析助手(Plan模式，只读分析)".to_string(),
        "yolo" => "你是一个强大的AI助手(YOLO模式，无限制)".to_string(),
        _ => "你是一个专业的编程助手(Agent模式)".to_string(),
    }
}
