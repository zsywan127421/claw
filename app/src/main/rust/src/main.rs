mod config;
mod api;
mod context;
mod engine;

use config::load_config;
use std::io::{self, Write};

const RESET: &str = "\x1b[0m";
const BOLD: &str = "\x1b[1m";
const BLUE: &str = "\x1b[34m";
const CYAN: &str = "\x1b[36m";
const YELLOW: &str = "\x1b[33m";
const GREEN: &str = "\x1b[32m";
const RED: &str = "\x1b[31m";

fn print_banner() {
    println!("\x1b[2J\x1b[H");
    println!("{}{}╔══════════════════════════════════════════════════╗{}", BLUE, BOLD, RESET);
    println!("{}{}║      DeepSeek-TUI Terminal v0.1.0               ║{}", BLUE, BOLD, RESET);
    println!("{}{}╚══════════════════════════════════════════════════╝{}", BLUE, RESET);
    println!();
    println!("{}Terminal UI for Android{}", YELLOW);
    println!();
}

fn print_help() {
    println!("{}{}可用命令:{}", BOLD, RESET);
    println!("  {}/help{}   - 显示帮助", GREEN, RESET);
    println!("  {}/mode{}   - 切换模式 (plan/agent/yolo)", GREEN, RESET);
    println!("  {}/clear{}  - 清屏", GREEN, RESET);
    println!("  {}/exit{}   - 退出", GREEN, RESET);
    println!();
    println!("{}{}模式说明:{}", BOLD, RESET);
    println!("  {}{}Plan{}   - 只读分析模式", YELLOW, BOLD, RESET);
    println!("  {}{}Agent{}  - 完整代理模式", GREEN, BOLD, RESET);
    println!("  {}{}YOLO{}   - 无限制模式", RED, BOLD, RESET);
    println!();
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    env_logger::Builder::from_env(env_logger::Env::default().default_filter_or("info")).init();
    print_banner();

    let config = load_config()?;
    if config.api.api_key.is_empty() {
        println!("{}⚠️  警告: DEEPSEEK_API_KEY 未设置{}", YELLOW, RESET);
        println!("请配置 API Key 以启用完整功能");
    } else {
        println!("{}✓{} API Key 已配置", GREEN, RESET);
        println!("模型: {}", config.api.model);
    }
    println!();

    let mut mode = "agent".to_string();
    let mut engine = engine::AgentEngine::new(config);

    loop {
        print!("{}({}){} {}DeepSeek{} > ", match mode.as_str() {
            "plan" => YELLOW, "yolo" => RED, _ => GREEN,
        }, mode, RESET, CYAN, RESET);
        io::stdout().flush()?;

        let mut input = String::new();
        if io::stdin().read_line(&mut input).is_err() { break; }
        let input = input.trim();
        if input.is_empty() { continue; }

        if input.starts_with('/') {
            match input.to_lowercase().as_str() {
                "/help" | "/h" => { print_help(); }
                "/clear" | "/cls" => { println!("\x1b[2J\x1b[H"); }
                "/mode" => { println!("当前模式: {}", mode); }
                "/mode plan" | "/mode p" => { mode = "plan".to_string(); println!("{}{}✓ 切换到 Plan 模式{}", YELLOW, BOLD, RESET); }
                "/mode agent" | "/mode a" => { mode = "agent".to_string(); println!("{}{}✓ 切换到 Agent 模式{}", GREEN, BOLD, RESET); }
                "/mode yolo" | "/mode y" => { mode = "yolo".to_string(); println!("{}{}✓ 切换到 YOLO 模式{}", RED, BOLD, RESET); }
                "/exit" | "/quit" => { println!("再见!"); break; }
                _ => { println!("{}未知命令: {}{}", RED, input, RESET); }
            }
            continue;
        }

        print!("{}  {}", CYAN, RESET);
        io::stdout().flush()?;

        match engine.process_message(input, &mode).await {
            Ok(response) => { println!("{}", response); }
            Err(e) => { println!("{}{}错误: {}{}", RED, BOLD, e, RESET); }
        }
        println!("{}", RESET);
    }
    Ok(())
}
