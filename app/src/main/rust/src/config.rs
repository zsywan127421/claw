use serde::{Deserialize, Serialize};
use std::fs;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Config {
    pub api: ApiConfig,
    pub workspace: WorkspaceConfig,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ApiConfig {
    pub api_key: String,
    pub base_url: String,
    pub model: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WorkspaceConfig {
    pub path: String,
}

impl Default for Config {
    fn default() -> Self {
        Self {
            api: ApiConfig {
                api_key: std::env::var("DEEPSEEK_API_KEY").unwrap_or_default(),
                base_url: "https://api.deepseek.com".to_string(),
                model: "deepseek-chat".to_string(),
            },
            workspace: WorkspaceConfig {
                path: dirs::home_dir().unwrap_or_default().join(".deepseek/workspace").to_string_lossy().to_string(),
            },
        }
    }
}

pub fn load_config() -> anyhow::Result<Config> {
    let paths = vec![
        std::path::PathBuf::from(".deepseek/config.toml"),
        dirs::home_dir().map(|p| p.join(".deepseek/config.toml")).unwrap_or_default(),
    ];
    for path in paths {
        if path.exists() {
            let content = fs::read_to_string(&path)?;
            return Ok(toml::from_str(&content)?);
        }
    }
    Ok(Config::default())
}
