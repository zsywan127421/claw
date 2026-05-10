use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Session {
    pub id: String,
    pub name: String,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
    pub mode: String,
    pub workspace_path: String,
    pub message_count: usize,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Checkpoint {
    pub id: String,
    pub session_id: String,
    pub description: String,
    pub created_at: DateTime<Utc>,
    pub message_count: usize,
}

pub struct SessionManager {
    sessions_dir: String,
}

impl SessionManager {
    pub fn new(workspace_path: &str) -> Self {
        Self {
            sessions_dir: format!("{}/.sessions", workspace_path),
        }
    }

    pub fn create_session(
        &self,
        name: &str,
        workspace_path: &str,
        mode: &str,
    ) -> Result<Session, String> {
        std::fs::create_dir_all(&self.sessions_dir)
            .map_err(|e| format!("Failed to create sessions dir: {}", e))?;

        let session = Session {
            id: Uuid::new_v4().to_string(),
            name: name.to_string(),
            created_at: Utc::now(),
            updated_at: Utc::now(),
            mode: mode.to_string(),
            workspace_path: workspace_path.to_string(),
            message_count: 0,
        };

        let path = format!("{}/{}.json", self.sessions_dir, session.id);
        let content = serde_json::to_string_pretty(&session)
            .map_err(|e| format!("Failed to serialize session: {}", e))?;

        std::fs::write(&path, content)
            .map_err(|e| format!("Failed to write session: {}", e))?;

        Ok(session)
    }

    pub fn list_sessions(&self) -> Result<Vec<Session>, String> {
        if !std::path::Path::new(&self.sessions_dir).exists() {
            return Ok(Vec::new());
        }

        let mut sessions = Vec::new();
        let entries = std::fs::read_dir(&self.sessions_dir)
            .map_err(|e| format!("Failed to read sessions dir: {}", e))?;

        for entry in entries.flatten() {
            if let Ok(content) = std::fs::read_to_string(entry.path()) {
                if let Ok(session) = serde_json::from_str::<Session>(&content) {
                    sessions.push(session);
                }
            }
        }

        sessions.sort_by(|a, b| b.updated_at.cmp(&a.updated_at));
        Ok(sessions)
    }

    pub fn get_session(&self, id: &str) -> Result<Session, String> {
        let path = format!("{}/{}.json", self.sessions_dir, id);
        let content = std::fs::read_to_string(&path)
            .map_err(|e| format!("Session not found: {}", e))?;

        serde_json::from_str(&content)
            .map_err(|e| format!("Failed to parse session: {}", e))
    }

    pub fn create_checkpoint(&self, session_id: &str, description: &str) -> Result<Checkpoint, String> {
        let session = self.get_session(session_id)?;

        let checkpoint = Checkpoint {
            id: Uuid::new_v4().to_string(),
            session_id: session.id.clone(),
            description: description.to_string(),
            created_at: Utc::now(),
            message_count: session.message_count,
        };

        let checkpoint_dir = format!("{}/checkpoints", self.sessions_dir);
        std::fs::create_dir_all(&checkpoint_dir)
            .map_err(|e| format!("Failed to create checkpoint dir: {}", e))?;

        let path = format!("{}/{}.json", checkpoint_dir, checkpoint.id);
        let content = serde_json::to_string_pretty(&checkpoint)
            .map_err(|e| format!("Failed to serialize checkpoint: {}", e))?;

        std::fs::write(&path, content)
            .map_err(|e| format!("Failed to write checkpoint: {}", e))?;

        Ok(checkpoint)
    }
}
