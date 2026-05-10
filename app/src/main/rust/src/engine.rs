use crate::api::{ChatRequest, DeepSeekClient, Message};
use crate::context::{ContextManager, create_system_prompt};
use crate::config::Config;

pub struct AgentEngine {
    config: Config,
    context: ContextManager,
    client: Option<DeepSeekClient>,
}

impl AgentEngine {
    pub fn new(config: Config) -> Self {
        let client = if !config.api.api_key.is_empty() {
            Some(DeepSeekClient::new(config.api.api_key.clone(), config.api.base_url.clone()))
        } else { None };
        Self { config, context: ContextManager::new(1_000_000), client }
    }

    pub async fn process_message(&mut self, user_message: &str, mode: &str) -> anyhow::Result<String> {
        self.context.clear();
        self.context.add_message("system", &create_system_prompt(mode));
        self.context.add_message("user", user_message);

        if let Some(ref client) = self.client {
            let messages = self.context.get_messages();
            let request = ChatRequest {
                model: self.config.api.model.clone(),
                messages,
                stream: None,
                temperature: Some(0.7),
            };
            match client.chat(request).await {
                Ok(response) => {
                    if let Some(choice) = response.choices.first() {
                        Ok(choice.message.content.clone())
                    } else { Ok("未收到响应".to_string()) }
                }
                Err(e) => Ok(format!("错误: {}", e)),
            }
        } else {
            Ok(format!("收到: {}\n\n请配置 DEEPSEEK_API_KEY\n访问 https://platform.deepseek.com/", user_message))
        }
    }
}
